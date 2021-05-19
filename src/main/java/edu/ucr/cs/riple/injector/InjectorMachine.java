package edu.ucr.cs.riple.injector;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;

import java.io.File;
import java.io.FileNotFoundException;
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
                    if(success) processed++;
                    log(workList.className(), !success);
                }
                overWriteToFile(tree, workList.getUri());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                log(workList.className(), false);
            }
        }
        return processed;
    }

    private boolean applyFix(CompilationUnit tree, Fix fix) {
        boolean success = false;
        ClassOrInterfaceDeclaration clazz = Helper.getClassOrInterfaceDeclaration(tree, fix.pkg, fix.className);
        if(clazz == null){
            return false;
        }
        switch (fix.location) {
            case "CLASS_FIELD":
                success = applyClassField(tree, clazz, fix);
                break;
            case "METHOD_RETURN":
                success = applyMethodReturn(tree, clazz, fix);
                break;
            case "METHOD_PARAM":
                success = applyMethodParam(tree, clazz, fix);
                break;
        }
        if (success) {
            if (Helper.getPackageName(fix.annotation) != null) {
                tree.addImport(fix.annotation);
            }
        }
        return success;
    }

    private static void applyAnnotation(NodeWithAnnotations<?> node, String annotName, boolean inject) {
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
        if(inject){
            if(!exists) {
                node.addMarkerAnnotation(annotSimpleName);
            }
        }else {
            annots.remove(existingAnnot);
        }
    }

    private boolean applyMethodParam(
            CompilationUnit tree, ClassOrInterfaceDeclaration clazz, Fix fix) {
        final boolean[] success = {false};
        NodeList<BodyDeclaration<?>> members = clazz.getMembers();
        members.forEach(bodyDeclaration -> bodyDeclaration.ifCallableDeclaration(callableDeclaration -> {
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

    private boolean applyMethodReturn(CompilationUnit tree, ClassOrInterfaceDeclaration clazz, Fix fix) {
        NodeList<BodyDeclaration<?>> members = clazz.getMembers();
        final boolean[] success = {false};
        members.forEach(
                bodyDeclaration -> bodyDeclaration.ifMethodDeclaration(
                        methodDeclaration -> {
                            if(Helper.matchesMethodSignature(methodDeclaration, fix.method)){
                                applyAnnotation(methodDeclaration, fix.annotation, Boolean.parseBoolean(fix.inject));
                                success[0] = true;
                            }
                        }));
        return success[0];
    }

    private boolean applyClassField(
            CompilationUnit tree, ClassOrInterfaceDeclaration clazz, Fix fix) {
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
                                            applyAnnotation(fieldDeclaration, fix.annotation, Boolean.parseBoolean(fix.inject));
                                            success[0] = true;
                                            break;
                                        }
                                    }
                                }));
        return success[0];
    }

    private void log(String className, boolean fail) {
        if (fail) System.out.print("\u001B[31m");
        else System.out.print("\u001B[32m");
        System.out.printf("Processing %-50s", Helper.simpleName(className));
        if (fail) System.out.println("✘ (Skipped)");
        else System.out.println("\u2713");
        System.out.print("\u001B[0m");
    }
}
