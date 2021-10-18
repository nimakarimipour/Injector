package edu.ucr.cs.riple.injector;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

public class Helper {

  public static String extractMethodName(String signature) {
    StringBuilder ans = new StringBuilder();
    int level = 0;
    for (int i = 0; i < signature.length(); i++) {
      char current = signature.charAt(i);
      if (current == '(') break;
      switch (current) {
        case '>':
          ++level;
          break;
        case '<':
          --level;
          break;
        default:
          if (level == 0) ans.append(current);
      }
    }
    return ans.toString();
  }

  public static boolean matchesMethodSignature(
      CallableDeclaration<MethodDeclaration> methodDecl, String signature) {
    if (!methodDecl.getName().toString().equals(extractMethodName(signature))) return false;
    List<String> paramsTypesInSignature = extractParamTypesOfMethodInString(signature);
    List<String> paramTypes = extractParamTypesOfMethodInString(methodDecl);
    if (paramTypes.size() != paramsTypesInSignature.size()) return false;
    for (String i : paramsTypesInSignature) {
      String found = null;
      String last_i = simpleName(i);
      for (String j : paramTypes) {
        String last_j = simpleName(j);
        if (j.equals(i) || last_i.equals(last_j)) found = j;
      }
      if (found == null) return false;
      paramTypes.remove(found);
    }
    return true;
  }

  public static List<String> extractParamTypesOfMethodInString(
      CallableDeclaration<MethodDeclaration> methodDecl) {
    ArrayList<String> paramTypes = new ArrayList<>();
    for (Parameter param : methodDecl.getParameters()) {
      if (param != null) {
        paramTypes.add(param.getType().asString());
      }
    }
    return paramTypes;
  }

  public static ClassOrInterfaceDeclaration getClassOrInterfaceDeclaration(
      CompilationUnit cu, String pkg, String name) {
    String classSimpleName = simpleName(name);
    if (pkg.equals(getPackageName(name))) {
      Optional<ClassOrInterfaceDeclaration> optional = cu.getClassByName(classSimpleName);
      if (optional.isEmpty()) {
        optional = cu.getInterfaceByName(classSimpleName);
        if (optional.isPresent()) {
          return optional.get();
        }
      }
    }
    try {
      List<ClassOrInterfaceDeclaration> options =
          cu.getLocalDeclarationFromClassname(classSimpleName);
      for (ClassOrInterfaceDeclaration candidate : options) {
        if (candidate.getName().toString().equals(classSimpleName)) {
          return candidate;
        }
      }
    } catch (NoSuchElementException ignored) {
    }
    List<ClassOrInterfaceDeclaration> candidates =
        cu.findAll(
            ClassOrInterfaceDeclaration.class,
            classOrInterfaceDeclaration ->
                classOrInterfaceDeclaration.getName().toString().equals(classSimpleName));
    if (candidates.size() > 0) {
      return candidates.get(0);
    }
    return null;
  }

  public static String simpleName(String name) {
    int index = 0;
    StringBuilder ans = new StringBuilder();
    StringBuilder tmp = new StringBuilder();
    while (index < name.length()) {
      char c = name.charAt(index);
      switch (c) {
        case ' ':
        case '<':
        case '>':
        case ',':
          ans.append(tmp);
          ans.append(c);
          tmp = new StringBuilder();
          break;
        case '.':
          tmp = new StringBuilder();
          break;
        default:
          tmp.append(c);
      }
      index++;
    }
    if (name.length() > 0) ans.append(tmp);
    return ans.toString().replaceAll(" ", "");
  }

  public static String getPackageName(String name) {
    if (!name.contains(".")) {
      return null;
    }
    int index = name.lastIndexOf(".");
    return name.substring(0, index);
  }

  public static List<String> extractParamTypesOfMethodInString(String signature) {
    signature = signature.substring(signature.indexOf("(")).replace("(", "").replace(")", "");
    int index = 0;
    int generic_level = 0;
    List<String> ans = new ArrayList<>();
    StringBuilder tmp = new StringBuilder();
    while (index < signature.length()) {
      char c = signature.charAt(index);
      switch (c) {
        case '@':
          while (signature.charAt(index + 1) == ' ' && index + 1 < signature.length()) index++;
          int annot_level = 0;
          boolean finished = false;
          while (!finished && index < signature.length()) {
            if (signature.charAt(index) == '(') ++annot_level;
            if (signature.charAt(index) == ')') --annot_level;
            if (signature.charAt(index) == ' ' && annot_level == 0) finished = true;
            index++;
          }
          index--;
          break;
        case '<':
          generic_level++;
          tmp.append(c);
          break;
        case '>':
          generic_level--;
          tmp.append(c);
          break;
        case ',':
          if (generic_level == 0) {
            ans.add(tmp.toString());
            tmp = new StringBuilder();
          } else tmp.append(c);
          break;
        default:
          tmp.append(c);
      }
      index++;
    }
    if (signature.length() > 0 && generic_level == 0) ans.add(tmp.toString());
    return ans;
  }
}
