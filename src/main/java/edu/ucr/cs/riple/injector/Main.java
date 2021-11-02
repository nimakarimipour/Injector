package edu.ucr.cs.riple.injector;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Main {

  private static Injector injector;

  public static void main(String[] args) {
//    injector = Injector.builder().setMode(Injector.MODE.BATCH).build();
//    Fix fix =
//        new Fix(
//            "com.badlogic.gdx.Initializer",
//            "format(java.lang.String,java.lang.Object...)",
//            "null",
//            "METHOD_RETURN",
//            "com.badlogic.gdx.utils.TextFormatter",
//            "/home/nima/Developer/AutoFixer/Evaluation/Projects/libgdx/gdx/src/com/badlogic/gdx/utils/TextFormatter.java",
//            "true");
//    fix.index = "0";
//    apply(Collections.singletonList(fix));
    findIssue();
  }

  private static void findIssue() {
    String path = "/home/nima/Developer/Injector/src/main/java/edu/ucr/cs/riple/injector/test/Test.java";
    CompilationUnit cu;
    try{
      cu = LexicalPreservingPrinter.setup(StaticJavaParser.parse(new File(path)));
    } catch (FileNotFoundException e){
      return;
    }
    TypeDeclaration<?> clazz = cu.getClassByName("Test").get();
    addAnnotation(cu, clazz, "setF", "Initializer", "edu.ucr.cs.riple.injector.Initializer");
    addAnnotation(cu, clazz, "run", "Nullable", "javax.annotation.Nullable");
    removeAnnotation(clazz, "run", "Nullable");
    addAnnotation(cu, clazz, "run", "Nullable", "javax.annotation.Nullable");
    System.out.println(LexicalPreservingPrinter.print(cu));
  }

  private static void addAnnotation(CompilationUnit cu, TypeDeclaration<?> clazz, String method, String simpleName, String fullName){
    clazz.getMembers().forEach(
            bodyDeclaration ->
                    bodyDeclaration.ifCallableDeclaration(
                            callableDeclaration -> {
                              if (callableDeclaration.getName().asString().equals(method)) {
                                callableDeclaration.addMarkerAnnotation(simpleName);
                              }
                            }));
    cu.addImport(fullName);
  }

  private static void removeAnnotation(TypeDeclaration<?> clazz, String method, String annot){
    clazz.getMembers().forEach(
            bodyDeclaration ->
                    bodyDeclaration.ifCallableDeclaration(
                            callableDeclaration -> {
                              if (callableDeclaration.getName().asString().equals(method)) {
                                NodeList<AnnotationExpr> annots = callableDeclaration.getAnnotations();
                                annots.removeIf(annotationExpr -> annotationExpr.getName().asString().equals(annot));
                              }
                            }));
  }

  public static void remove(List<Fix> fixes) {
    if (fixes == null || fixes.size() == 0) {
      return;
    }
    List<Fix> toRemove =
        fixes
            .stream()
            .map(
                fix ->
                    new Fix(
                        fix.annotation,
                        fix.method,
                        fix.param,
                        fix.location,
                        fix.className,
                        fix.uri,
                        "false"))
            .collect(Collectors.toList());
    apply(toRemove);
  }

  public static void apply(List<Fix> fixes) {
    if (fixes == null || fixes.size() == 0) {
      return;
    }
    injector.start(new WorkListBuilder(fixes).getWorkLists(), true);
  }
}
