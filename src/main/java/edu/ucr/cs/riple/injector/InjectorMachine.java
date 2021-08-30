package edu.ucr.cs.riple.injector;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class InjectorMachine {

  List<WorkList> workLists;
  Injector.MODE mode;
  int processed = 0;

  public InjectorMachine(List<WorkList> workLists, Injector.MODE mode) {
    this.workLists = workLists;
    this.mode = mode;
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
    for (WorkList workList : workLists) {
      try {
        tree = LexicalPreservingPrinter.setup(StaticJavaParser.parse(new File(workList.getUri())));
        for (Fix fix : workList.getFixes()) {
          boolean success = applyFix(tree, fix);
          if (success) {
            processed++;
          }
          log(fix.inject, workList.className(), fix.method, fix.param, fix.location, !success);
        }
        overWriteToFile(tree, workList.getUri());
      } catch (Exception e) {
        failedLog(workList.className());
      }
    }
    return processed;
  }

  private boolean applyFix(CompilationUnit tree, Fix fix) {
    boolean success = false;
    ClassOrInterfaceDeclaration clazz =
        Helper.getClassOrInterfaceDeclaration(tree, fix.pkg, fix.className);
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

  private boolean applyMethodParam(ClassOrInterfaceDeclaration clazz, Fix fix) {
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

  private boolean applyMethodReturn(ClassOrInterfaceDeclaration clazz, Fix fix) {
    NodeList<BodyDeclaration<?>> members = clazz.getMembers();
    final boolean[] success = {false};
    members.forEach(
        bodyDeclaration ->
            bodyDeclaration.ifMethodDeclaration(
                methodDeclaration -> {
                  if (Helper.matchesMethodSignature(methodDeclaration, fix.method)) {
                    applyAnnotation(
                        methodDeclaration, fix.annotation, Boolean.parseBoolean(fix.inject));
                    success[0] = true;
                  }
                }));
    return success[0];
  }

  private boolean applyClassField(ClassOrInterfaceDeclaration clazz, Fix fix) {
    class FieldDeclInfo {
      private boolean required = false;
      private Type type;
      private String name;
      private Node node;
      private FieldDeclaration fieldDeclaration;
      private NodeList<Modifier> modifiers;
      private int index;
    }
    final int[] index = {0};
    FieldDeclInfo fieldDeclInfo = new FieldDeclInfo();
    final boolean[] success = {false};
    NodeList<BodyDeclaration<?>> members = clazz.getMembers();
    members.forEach(
        bodyDeclaration -> {
          index[0]++;
          if (bodyDeclaration.isFieldDeclaration()) {
            bodyDeclaration.ifFieldDeclaration(
                fieldDeclaration -> {
                  NodeList<VariableDeclarator> vars =
                      fieldDeclaration.asFieldDeclaration().getVariables();
                  for (VariableDeclarator v : vars) {
                    if (v.getName().toString().equals(fix.param)) {
                      if (vars.size() > 1) {
                        fieldDeclInfo.required = true;
                        fieldDeclInfo.type = v.getType();
                        fieldDeclInfo.name = v.getName().asString();
                        fieldDeclInfo.index = index[0];
                        fieldDeclInfo.node = v;
                        fieldDeclInfo.fieldDeclaration = fieldDeclaration;
                        fieldDeclInfo.modifiers = fieldDeclaration.getModifiers();
                      } else {
                        applyAnnotation(
                            fieldDeclaration, fix.annotation, Boolean.parseBoolean(fix.inject));
                        success[0] = true;
                      }
                      break;
                    }
                  }
                });
          }
        });
    if (fieldDeclInfo.required) {
      FieldDeclaration fieldDeclaration = new FieldDeclaration();
      VariableDeclarator variable = new VariableDeclarator(fieldDeclInfo.type, fieldDeclInfo.name);
      fieldDeclaration.getVariables().add(variable);
      fieldDeclaration.setModifiers(
          Modifier.createModifierList(
              fieldDeclInfo
                  .modifiers
                  .stream()
                  .map(Modifier::getKeyword)
                  .distinct()
                  .toArray(Modifier.Keyword[]::new)));
      clazz.getMembers().add(fieldDeclInfo.index, fieldDeclaration);
      fieldDeclInfo.fieldDeclaration.remove(fieldDeclInfo.node);
      applyAnnotation(
          fieldDeclaration.asFieldDeclaration(), fix.annotation, Boolean.parseBoolean(fix.inject));
      success[0] = true;
    }
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

  private void log(
      String inject, String className, String method, String param, String location, boolean fail) {
    inject = inject.equals("true") ? "Injecting :" : "Removing  :";
    method = method.contains("(") ? method.substring(0, method.indexOf("(")) : method;
    className = Helper.simpleName(className);
    className = className.length() > 30 ? className.substring(0, 26) + "..." : className;
    method = method.length() > 25 ? method.substring(0, 21) + "..." : method;
    if (Injector.LOG) {
      if (fail) System.out.print("\u001B[31m");
      else System.out.print("\u001B[32m");
      System.out.printf(inject + " %-30s %-25s %-20s %-10s ", className, method, param, location);
      if (fail) System.out.println("✘ (Skipped)");
      else System.out.println("\u2713");
      System.out.print("\u001B[0m");
    }
  }
}
