package edu.ucr.cs.riple.injector.util;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.AnnotationExpr;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Utility {

  public static Set<String> findAllAnnots(Path path) {
    Set<String> ans = new HashSet<>();
    try {
      CompilationUnit tree = StaticJavaParser.parse(path);
      List<AnnotationExpr> annots = tree.findAll(AnnotationExpr.class);
      ans =
          annots
              .stream()
              .map(annotationExpr -> annotationExpr.getName().asString())
              .collect(Collectors.toSet());
    } catch (IOException e) {
      System.out.println("Exception happened: " + e.getMessage());
    }
    return ans;
  }
}
