package edu.ucr.cs.riple.injector;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.printer.DefaultPrettyPrinter;
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration;
import com.github.javaparser.printer.configuration.PrinterConfiguration;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import java.io.File;
import java.io.FileNotFoundException;
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
import org.json.simple.parser.ParseException;

public class InjectorMachine {

  List<WorkList> workLists;
  Injector.MODE mode;
  boolean keep;
  int processed = 0;
  int total = 0;
  final DefaultPrettyPrinter printer;

  public InjectorMachine(List<WorkList> workLists, Injector.MODE mode, boolean keep) {
    this.workLists = workLists;
    this.mode = mode;
    this.keep = keep;
    workLists.forEach(workList -> total += workList.getFixes().size());
    PrinterConfiguration configuration = new DefaultPrinterConfiguration();
    printer = new DefaultPrettyPrinter(configuration);
  }

  private void overWriteToFile(CompilationUnit changed, String uri) {
    if (mode.equals(Injector.MODE.TEST)) {
      uri = uri.replace("src", "out");
    }
    String pathToFileDirectory = uri.substring(0, uri.lastIndexOf("/"));
    try {
      Files.createDirectories(Paths.get(pathToFileDirectory + "/"));
      FileWriter writer = new FileWriter(uri);
      String toWrite = keep ? LexicalPreservingPrinter.print(changed) : printer.print(changed);
      writer.write(toWrite);
      writer.flush();
      writer.close();
    } catch (IOException e) {
      throw new RuntimeException("Something terrible happened.");
    }
  }

  public Integer start() {
    ProgressBar pb = createProgressBar("Injector", total);
    for (WorkList workList : workLists) {
      CompilationUnit tree;
      try {
        CompilationUnit tmp = StaticJavaParser.parse(new File(workList.getUri()));
        tree = keep ? LexicalPreservingPrinter.setup(tmp) : tmp;
      } catch (FileNotFoundException exception) {
        continue;
      }
      for (Fix fix : workList.getFixes()) {
        try {
          if (Injector.LOG) {
            pb.step();
          }
          boolean success = applyFix(tree, fix);
          if (success && Injector.LOG) {
            processed++;
            logSuccessful(fix);
          } else {
            logFailed(fix);
          }
        } catch (Exception ignored) {
          logFailed(fix);
        }
      }
      overWriteToFile(tree, workList.getUri());
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
        ImportDeclaration importDeclaration =
            StaticJavaParser.parseImport("import " + fix.annotation + ";");
        if (!tree.getImports().contains(importDeclaration)) {
          tree.getImports().addFirst(importDeclaration);
        }
      }
    }
    return success;
  }

  private static void applyAnnotation(
      NodeWithAnnotations<?> node, String annotName, boolean inject) {
    final String annotSimpleName = Helper.simpleName(annotName);
    NodeList<AnnotationExpr> annots = node.getAnnotations();
    boolean exists =
        annots
            .stream()
            .anyMatch(
                annot -> {
                  String thisAnnotName = annot.getNameAsString();
                  return thisAnnotName.equals(annotName) || thisAnnotName.equals(annotSimpleName);
                });
    if (inject && !exists) {
      node.addMarkerAnnotation(annotSimpleName);
    }
    if (!inject) {
      annots.removeIf(
          annot -> {
            String thisAnnotName = annot.getNameAsString();
            return thisAnnotName.equals(annotName) || thisAnnotName.equals(annotSimpleName);
          });
    }
  }

  private boolean applyMethodParam(TypeDeclaration<?> clazz, Fix fix) {
    final boolean[] success = {false};
    NodeList<BodyDeclaration<?>> members = clazz.getMembers();
    members.forEach(
        bodyDeclaration ->
            bodyDeclaration.ifCallableDeclaration(
                callableDeclaration -> {
                  if (Helper.matchesCallableSignature(callableDeclaration, fix.method)) {
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
                  if (Helper.matchesCallableSignature(callableDeclaration, fix.method)) {
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

  public ProgressBar createProgressBar(String task, int steps) {
    return new ProgressBar(
        task,
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
    appendAFix(fix, path);
  }

  private void logSuccessful(Fix fix) {
    final String path = "/tmp/NullAwayFix/injected.json";
    appendAFix(fix, path);
  }

  private void appendAFix(Fix fix, String path) {
    JSONObject json;
    try {
      File file = new File(path);
      if (!file.exists()) {
        if (!file.createNewFile()) {
          throw new RuntimeException("Could not create a new file at path: " + path);
        }
      }
      if(file.length() == 0){
        JSONArray fixes = new JSONArray();
        JSONObject ans = new JSONObject();
        ans.put("fixes", fixes);
        FileWriter w = new FileWriter(path);
        w.write(ans.toJSONString());
        w.close();
      }
      json = (JSONObject) new JSONParser().parse(new FileReader(path));
    }
    catch (Exception e) {
      throw new RuntimeException(path, e);
    }
    assert new File(path).exists();
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
