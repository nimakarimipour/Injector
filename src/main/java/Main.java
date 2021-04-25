import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;

public class Main {

    static final String path1 = "/Users/nima/Developer/ArtifactEvaluation/NullAwayFixer/Projects/libgdx/gdx/src/com/badlogic/gdx/scenes/scene2d/ui/Window.java";
    static final String path2 = "/Users/nima/Developer/ArtifactEvaluation/NullAwayFixer/Projects/libgdx/gdx/src/com/badlogic/gdx/utils/XmlReader.java";
    static final String path3 = "/Users/nima/Developer/ArtifactEvaluation/NullAwayFixer/Projects/libgdx/gdx/src/com/badlogic/gdx/utils/JsonReader.java";

    public static void main(String[] args) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(new File(path2));
            method(cu);
            write(cu);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    static void method(CompilationUnit cu) {
        Optional<ClassOrInterfaceDeclaration> optionalClassCheck = cu.getClassByName("XmlReader");
        if (optionalClassCheck.isPresent()) {
            ClassOrInterfaceDeclaration clazz = optionalClassCheck.get();
            NodeList<BodyDeclaration<?>> members = clazz.getMembers();
            members.forEach(new Consumer<BodyDeclaration<?>>() {
                @Override
                public void accept(BodyDeclaration<?> bodyDeclaration) {
                    bodyDeclaration.ifMethodDeclaration(new Consumer<MethodDeclaration>() {
                        @Override
                        public void accept(MethodDeclaration methodDeclaration) {
                            System.out.println(methodDeclaration.getDeclarationAsString(false, false, true));
                            NodeList<Parameter> params = methodDeclaration.getParameters();
                            for(Parameter p: params){
                                p.addAnnotation("Nullable");
                                System.out.println("P: " + p.getType());
                                System.out.println("P: " + p.getName());
                            }
                            methodDeclaration.addAnnotation("nullable");
                        }
                    });
                }
            });
        }
    }

    static void field(CompilationUnit cu) {
        cu.addImport("java.io.Reader");
        Optional<ClassOrInterfaceDeclaration> optionalClassCheck = cu.getClassByName("XmlReader");
        if (optionalClassCheck.isPresent()) {
            ClassOrInterfaceDeclaration clazz = optionalClassCheck.get();
            NodeList<BodyDeclaration<?>> members = clazz.getMembers();
            members.forEach(bodyDeclaration -> bodyDeclaration.ifFieldDeclaration(fieldDeclaration -> fieldDeclaration.addAnnotation("nullable")));
        }
    }

    static void write(CompilationUnit cu){
        try {
            FileWriter myWriter = new FileWriter("./res.java");
            myWriter.write(cu.toString());
            myWriter.close();
            System.out.println("Successfully wrote to the file.");
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
}
