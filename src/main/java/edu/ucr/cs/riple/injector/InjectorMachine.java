package edu.ucr.cs.riple.injector;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarStyle;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class InjectorMachine {

  List<WorkList> workLists;
  Injector.MODE mode;
  int processed = 0;
  int total = 0;

  public InjectorMachine(List<WorkList> workLists, Injector.MODE mode) {
    this.workLists = workLists;
    this.mode = mode;
    workLists.forEach(workList -> total += workList.getFixes().size());
  }

  private void overWriteToFile(CompilationUnit changed, String uri) {
    if (mode.equals(Injector.MODE.TEST)) {
      uri = uri.replace("src", "out");
    }
    String pathToFileDirectory = uri.substring(0, uri.lastIndexOf("/"));
    try {
      Files.createDirectories(Paths.get(pathToFileDirectory + "/"));
      FileWriter writer = new FileWriter(uri);
      writer.write(LexicalPreservingPrinter.print(changed));
      writer.flush();
      writer.close();
    } catch (IOException e) {
      throw new RuntimeException("Something terrible happened.");
    }
  }

  public Integer start() {
    CompilationUnit tree;
    ProgressBar pb = createProgressBar(total);
    Fix theFix = null;
    for (WorkList workList : workLists) {
      try {
        tree = LexicalPreservingPrinter.setup(StaticJavaParser.parse(new File(workList.getUri())));
        for (Fix fix : workList.getFixes()) {
          theFix = fix;
          if (Injector.LOG) {
            pb.step();
          }
          boolean success = applyFix(tree, fix);
          if (success) {
            processed++;
          } else {
            logFailed(fix);
          }
        }
        overWriteToFile(tree, workList.getUri());
      } catch (Exception e) {
        failedLog(workList.className());
        logFailed(theFix);
      }
    }
    if (Injector.LOG) {
      pb.stepTo(total);
    }
    pb.close();
    return processed;
  }

  private boolean applyFix(CompilationUnit tree, Fix fix) {
    boolean success = false;
    TypeDeclaration<?> clazz =
        Helper.getClassOrInterfaceOrEnumDeclaration(tree, fix.pkg, fix.className);
    if (clazz == null) {
      return false;
    }
    switch (fix.location) {
      case "CLASS_FIELD":
        success = applyClassField(clazz, fix);
        break;
      case "METHOD_RETURN":
        success = applyMethodReturn(clazz, fix);
        break;
      case "METHOD_PARAM":
        success = applyMethodParam(clazz, fix);
        break;
    }
    if (success) {
      if (Helper.getPackageName(fix.annotation) != null) {
        tree.addImport(fix.annotation);
      }
    }
    return success;
  }

  private static void applyAnnotation(
      NodeWithAnnotations<?> node, String annotName, boolean inject) {
    final String annotSimpleName = Helper.simpleName(annotName);
    NodeList<AnnotationExpr> annots = node.getAnnotations();
    AnnotationExpr existingAnnot = null;
    boolean exists = false;
    for (AnnotationExpr annot : annots) {
      String thisAnnotName = annot.getNameAsString();
      if (thisAnnotName.equals(annotName) || thisAnnotName.equals(annotSimpleName)) {
        exists = true;
        existingAnnot = annot;
      }
    }
    if (inject) {
      if (!exists) {
        node.addMarkerAnnotation(annotSimpleName);
      }
    } else {
      annots.remove(existingAnnot);
    }
  }

  private boolean applyMethodParam(TypeDeclaration<?> clazz, Fix fix) {
    final boolean[] success = {false};
    NodeList<BodyDeclaration<?>> members = clazz.getMembers();
    members.forEach(
        bodyDeclaration ->
            bodyDeclaration.ifCallableDeclaration(
                callableDeclaration -> {
                  if (Helper.matchesMethodSignature(callableDeclaration, fix.method)) {
                    for (Object p : callableDeclaration.getParameters()) {
                      if (p instanceof Parameter) {
                        Parameter param = (Parameter) p;
                        if (param.getName().toString().equals(fix.param)) {
                          applyAnnotation(param, fix.annotation, Boolean.parseBoolean(fix.inject));
                          success[0] = true;
                        }
                      }
                    }
                  }
                }));
    return success[0];
  }

  private boolean applyMethodReturn(TypeDeclaration<?> clazz, Fix fix) {
    NodeList<BodyDeclaration<?>> members = clazz.getMembers();
    final boolean[] success = {false};
    members.forEach(
        bodyDeclaration ->
            bodyDeclaration.ifCallableDeclaration(
                callableDeclaration -> {
                  if (Helper.matchesMethodSignature(callableDeclaration, fix.method)) {
                    applyAnnotation(
                        callableDeclaration, fix.annotation, Boolean.parseBoolean(fix.inject));
                    success[0] = true;
                  }
                }));
    return success[0];
  }

  private boolean applyClassField(TypeDeclaration<?> clazz, Fix fix) {
    final boolean[] success = {false};
    NodeList<BodyDeclaration<?>> members = clazz.getMembers();
    members.forEach(
        bodyDeclaration ->
            bodyDeclaration.ifFieldDeclaration(
                fieldDeclaration -> {
                  NodeList<VariableDeclarator> vars =
                      fieldDeclaration.asFieldDeclaration().getVariables();
                  for (VariableDeclarator v : vars) {
                    if (v.getName().toString().equals(fix.param)) {
                      applyAnnotation(
                          fieldDeclaration, fix.annotation, Boolean.parseBoolean(fix.inject));
                      success[0] = true;
                      break;
                    }
                  }
                }));

    return success[0];
  }

  private void failedLog(String className) {
    if (Injector.LOG) {
      System.out.print("\u001B[31m");
      System.out.printf("Processing: %-90s", Helper.simpleName(className));
      System.out.println("✘ (Skipped)");
      System.out.print("\u001B[0m");
    }
  }

  private ProgressBar createProgressBar(int steps) {
    return new ProgressBar(
        "Injector",
        steps,
        1000,
        System.out,
        ProgressBarStyle.ASCII,
        "",
        1,
        false,
        null,
        ChronoUnit.SECONDS,
        0L,
        Duration.ZERO);
  }

  @SuppressWarnings("ALL")
  private void logFailed(Fix fix) {
    final String path = "/tmp/NullAwayFix/failed.json";
    JSONObject json = null;
    try {
      json = (JSONObject) new JSONParser().parse(new FileReader(path));
    } catch (Exception e) {
      e.printStackTrace();
    }
    assert json != null;
    JSONArray all = (JSONArray) json.get("fixes");
    all.add(fix.getJson());
    JSONObject toWrite = new JSONObject();
    toWrite.put("fixes", all);
    try {
      Files.write(Paths.get(path), toWrite.toJSONString().getBytes());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
