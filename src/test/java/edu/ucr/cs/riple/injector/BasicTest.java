package edu.ucr.cs.riple.injector;

import edu.ucr.cs.riple.injector.tools.InjectorTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class BasicTest {
  InjectorTestHelper injectorTestHelper;

  @Before
  public void setup() {}

  @Test
  public void return_nullable_simple() {
    String rootName = "return_nullable_simple";

    injectorTestHelper =
        new InjectorTestHelper()
            .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
            .addInput(
                "Super.java",
                "package com.uber;",
                "public class Super {",
                "   Object test(boolean flag) {",
                "       return new Object();",
                "   }",
                "}")
            .expectOutput(
                "Super.java",
                "package com.uber;",
                "import javax.annotation.Nullable;",
                "public class Super {",
                "   @Nullable()",
                "   Object test(boolean flag) {",
                "       return new Object();",
                "   }",
                "}")
            .addInput(
                "com/Superb.java",
                "package com.uber;",
                "public class Superb extends Super {",
                "   Object test(boolean flag) {",
                "       return new Object();",
                "   }",
                "}")
            .expectOutput(
                "com/Superb.java",
                "package com.uber;",
                "import javax.annotation.Nullable;",
                "public class Superb extends Super{",
                "   @Nullable()",
                "   Object test(boolean flag) {",
                "       return new Object();",
                "   }",
                "}")
            .addFixes(
                new Fix(
                    "javax.annotation.Nullable",
                    "test(boolean)",
                    "",
                    "METHOD_RETURN",
                    "com.uber.Super",
                    "com.uber",
                    "Super.java",
                    "true"),
                new Fix(
                    "javax.annotation.Nullable",
                    "test(boolean)",
                    "",
                    "METHOD_RETURN",
                    "com.uber.Superb",
                    "com.uber",
                    "com/Superb.java",
                    "true"));
    injectorTestHelper.start();
    injectorTestHelper = null;
  }

  @Test
  public void return_nullable_inner_class() {
    String rootName = "return_nullable_inner_class";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   @Nullable()",
            "   Object test(boolean flag) {",
            "       return new Object();",
            "   }",
            "   class SuperInner {",
            "       Object bar(@Nullable() Object foo) {",
            "           return foo;",
            "       }",
            "   }",
            "}")
        .expectOutput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   @Nullable()",
            "   Object test(boolean flag) {",
            "       return new Object();",
            "   }",
            "   class SuperInner {",
            "       @Nullable()",
            "       Object bar(@Nullable() Object foo) {",
            "           return foo;",
            "       }",
            "   }",
            "}")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "bar(java.lang.Object)",
                "",
                "METHOD_RETURN",
                "com.uber.Super.SuperInner",
                "com.uber",
                "Super.java",
                "true"))
        .start();
  }

  @Test
  public void return_nullable_signature_duplicate_type() {
    String rootName = "return_nullable_signature_duplicate_type";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInput(
            "Super.java",
            "package com.uber;",
            "public class Super {",
            "   Object test(Object flag, String name, String lastname) {",
            "       if(flag == null) {",
            "           return new Object();",
            "       }",
            "       else return new Object();",
            "   }",
            "   Object test(Object flag, Object name, String lastname) {",
            "       if(flag == null) {",
            "           return new Object();",
            "       }",
            "       else return new Object();",
            "   }",
            "}")
        .expectOutput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   @Nullable() Object test(Object flag, String name, String lastname) {",
            "       if(flag == null) {",
            "           return new Object();",
            "       }",
            "       else return new Object();",
            "   }",
            "   Object test(Object flag, @Nullable() Object name, String lastname) {",
            "       if(flag == null) {",
            "           return new Object();",
            "       }",
            "       else return new Object();",
            "   }",
            "}")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "test(Object, String, String)",
                "",
                "METHOD_RETURN",
                "com.uber.Super",
                "com.uber",
                "Super.java",
                "true"),
            new Fix(
                "javax.annotation.Nullable",
                "test(Object, Object, String)",
                "name",
                "METHOD_PARAM",
                "com.uber.Super",
                "com.uber",
                "Super.java",
                "true"))
        .start();
  }

  @Test
  public void return_nullable_single_generic_method_pick() {
    String rootName = "return_nullable_single_generic_method_pick";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInput(
            "Super.java",
            "package com.uber;",
            "public class Super {",
            "   public IntSet getPredNodeNumbers(T node) throws UnimplementedError {",
            "       Assertions.UNREACHABLE();",
            "       return null;",
            "   }",
            "  OrdinalSet<Statement> computeResult(",
            "        Statement s,",
            "        Map<PointerKey, MutableIntSet> pointerKeyMod,",
            "        BitVectorSolver<? extends ISSABasicBlock> solver,",
            "        OrdinalSetMapping<Statement> domain,",
            "        CGNode node,",
            "        ExtendedHeapModel h,",
            "        PointerAnalysis<T> pa,",
            "        Map<CGNode, OrdinalSet<PointerKey>> mod,",
            "        ExplodedControlFlowGraph cfg,",
            "        Map<Integer, NormalStatement> ssaInstructionIndex2Statement) {",
            "     return null;",
            "   }",
            "}")
        .expectOutput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   @Nullable()",
            "   public IntSet getPredNodeNumbers(T node) throws UnimplementedError {",
            "       Assertions.UNREACHABLE();",
            "       return null;",
            "   }",
            "  @Nullable() OrdinalSet<Statement> computeResult(",
            "        Statement s,",
            "        Map<PointerKey, MutableIntSet> pointerKeyMod,",
            "        BitVectorSolver<? extends ISSABasicBlock> solver,",
            "        OrdinalSetMapping<Statement> domain,",
            "        CGNode node,",
            "        ExtendedHeapModel h,",
            "        PointerAnalysis<T> pa,",
            "        Map<CGNode, OrdinalSet<PointerKey>> mod,",
            "        ExplodedControlFlowGraph cfg,",
            "        Map<Integer, NormalStatement> ssaInstructionIndex2Statement) {",
            "     return null;",
            "   }",
            "}")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "getPredNodeNumbers(T)",
                "",
                "METHOD_RETURN",
                "com.uber.Super",
                "com.uber",
                "Super.java",
                "true"),
            new Fix(
                "javax.annotation.Nullable",
                "computeResult(com.ibm.wala.ipa.slicer.Statement,java.util.Map<com.ibm.wala.ipa.callgraph.propagation.PointerKey,com.ibm.wala.util.intset.MutableIntSet>,com.ibm.wala.dataflow.graph.BitVectorSolver<? extends com.ibm.wala.ssa.ISSABasicBlock>,com.ibm.wala.util.intset.OrdinalSetMapping<com.ibm.wala.ipa.slicer.Statement>,com.ibm.wala.ipa.callgraph.CGNode,com.ibm.wala.ipa.modref.ExtendedHeapModel,com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis<T>,java.util.Map<com.ibm.wala.ipa.callgraph.CGNode,com.ibm.wala.util.intset.OrdinalSet<com.ibm.wala.ipa.callgraph.propagation.PointerKey>>,com.ibm.wala.ssa.analysis.ExplodedControlFlowGraph,java.util.Map<java.lang.Integer,com.ibm.wala.ipa.slicer.NormalStatement>)",
                "",
                "METHOD_RETURN",
                "com.uber.Super",
                "com.uber",
                "Super.java",
                "true"))
        .start();
  }

  @Test
  public void return_nullable_signature_array_brackets() {
    String rootName = "return_nullable_signature_array_brackets";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInput(
            "Super.java",
            "package com.uber;",
            "public class Super {",
            "   protected CGNode getTargetForCall(",
            "     CGNode caller[], CallSiteReference[][][] site, IClass recv, InstanceKey iKey[][]) {",
            "     return null;",
            "   }",
            "}")
        .expectOutput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   @Nullable() protected CGNode getTargetForCall(",
            "     CGNode[] caller, CallSiteReference[][][] site, IClass recv, InstanceKey[][] iKey) {",
            "     return null;",
            "   }",
            "}")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "getTargetForCall(com.ibm.wala.ipa.callgraph.CGNode[],com.ibm.wala.classLoader.CallSiteReference[][][],com.ibm.wala.classLoader.IClass,com.ibm.wala.ipa.callgraph.propagation.InstanceKey[][])",
                "",
                "METHOD_RETURN",
                "com.uber.Super",
                "com.uber",
                "Super.java",
                "true"))
        .start();
  }

  @Test
  public void return_nullable_signature_generic_method_name() {
    String rootName = "return_nullable_signature_generic_method_name";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInput(
            "Super.java",
            "package com.uber;",
            "public class Super {",
            "   static <T> T getReader(ClassReader.AttrIterator iter, String attrName, GetReader<T> reader) {",
            "     return null;",
            "   }",
            "}")
        .expectOutput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   @Nullable() static <T> T getReader(ClassReader.AttrIterator iter, String attrName, GetReader<T> reader) {",
            "     return null;",
            "   }",
            "}")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "<T>getReader(com.ibm.wala.shrikeCT.ClassReader.AttrIterator,java.lang.String,com.ibm.wala.classLoader.ShrikeClass.GetReader<T>)",
                "",
                "METHOD_RETURN",
                "com.uber.Super",
                "com.uber",
                "Super.java",
                "true"))
        .start();
  }

  @Test
  public void return_nullable_class_declaration_in_method_body() {
    String rootName = "return_nullable_class_declaration_in_method_body";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInput(
            "TargetMethodContextSelector.java",
            "package com.uber;",
            "public class TargetMethodContextSelector implements ContextSelector {",
            "   @Override",
            "   public Context getCalleeTarget() {",
            "     class MethodDispatchContext implements Context {",
            "        @Override",
            "        public ContextItem get(ContextKey name) { }",
            "     }",
            "   }",
            "}")
        .expectOutput(
            "TargetMethodContextSelector.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class TargetMethodContextSelector implements ContextSelector {",
            "   @Override",
            "   public Context getCalleeTarget() {",
            "     class MethodDispatchContext implements Context {",
            "        @Override @Nullable()",
            "         public ContextItem get(ContextKey name) { }",
            "     }",
            "   }",
            "}")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "get(com.ibm.wala.ipa.callgraph.ContextKey)",
                "",
                "METHOD_RETURN",
                "com.uber.MethodDispatchContext",
                "com.uber",
                "TargetMethodContextSelector.java",
                "true"))
        .start();
  }

  @Test
  public void param_nullable_simple() {
    String rootName = "param_nullable_simple";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   @Nullable() Object test(Object flag) {",
            "       if(flag == null) {",
            "           return new Object();",
            "       }",
            "       else return new Object();",
            "   }",
            "}")
        .expectOutput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   @Nullable() Object test(@Nullable() Object flag) {",
            "       if(flag == null) {",
            "           return new Object();",
            "       }",
            "       else return new Object();",
            "   }",
            "}")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "test(java.lang.Object)",
                "flag",
                "METHOD_PARAM",
                "com.uber.Super",
                "com.uber",
                "Super.java",
                "true"))
        .start();
  }

  @Test
  public void param_nullable_signature_incomplete() {
    String rootName = "param_nullable_signature_incomplete";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   @Nullable() Object test(Object flag) {",
            "       if(flag == null) {",
            "           return new Object();",
            "       }",
            "       else return new Object();",
            "   }",
            "}")
        .expectOutput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   @Nullable() Object test(@Nullable() Object flag) {",
            "       if(flag == null) {",
            "           return new Object();",
            "       }",
            "       else return new Object();",
            "   }",
            "}")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "test(Object)",
                "flag",
                "METHOD_PARAM",
                "com.uber.Super",
                "com.uber",
                "Super.java",
                "true"))
        .start();
  }

  @Test
  public void param_nullable_interface() {
    String rootName = "param_nullable_interface";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInput(
            "SSAInstructionFactory.java",
            "package com.uber;",
            "public interface SSAInstructionFactory {",
            "SSAAbstractInvokeInstruction InvokeInstruction(",
            "   int index,",
            "   int result,",
            "   int[] params,",
            "   int exception,",
            "   CallSiteReference site,",
            "   BootstrapMethod bootstrap);",
            "",
            "SSAAbstractInvokeInstruction InvokeInstruction(",
            "   int index, int[] params, int exception, CallSiteReference site, BootstrapMethod bootstrap);",
            "}")
        .expectOutput(
            "SSAInstructionFactory.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public interface SSAInstructionFactory {",
            "SSAAbstractInvokeInstruction InvokeInstruction(",
            "   int index,",
            "   int result,",
            "   int[] params,",
            "   int exception,",
            "   CallSiteReference site,",
            "   @Nullable() BootstrapMethod bootstrap);",
            "",
            "SSAAbstractInvokeInstruction InvokeInstruction(",
            "   int index, int[] params, int exception, CallSiteReference site, BootstrapMethod bootstrap);",
            "}")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "InvokeInstruction(int,int,int[],int,com.ibm.wala.classLoader.CallSiteReference,com.ibm.wala.shrikeCT.BootstrapMethodsReader.BootstrapMethod)",
                "bootstrap",
                "METHOD_PARAM",
                "com.uber.SSAInstructionFactory",
                "com.uber",
                "SSAInstructionFactory.java",
                "true"))
        .start();
  }

  @Test
  public void param_nullable_generics_simple() {
    String rootName = "param_nullable_generics_simple";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInput(
            "ModRef.java",
            "package com.uber;",
            "public class ModRef<T extends InstanceKey> {",
            "   public Map<CGNode, OrdinalSet<PointerKey>> computeMod(",
            "     CallGraph cg, PointerAnalysis<T> pa, HeapExclusions heapExclude) {",
            "     if (cg == null) {",
            "       throw new IllegalArgumentException(\"cg is null\");",
            "     }",
            "     Map<CGNode, Collection<PointerKey>> scan = scanForMod(cg, pa, heapExclude);",
            "     return CallGraphTransitiveClosure.transitiveClosure(cg, scan);",
            "   }",
            "}")
        .expectOutput(
            "ModRef.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class ModRef<T extends InstanceKey> {",
            "   public Map<CGNode, OrdinalSet<PointerKey>> computeMod(",
            "     CallGraph cg, PointerAnalysis<T> pa, @Nullable() HeapExclusions heapExclude) {",
            "     if (cg == null) {",
            "       throw new IllegalArgumentException(\"cg is null\");",
            "     }",
            "     Map<CGNode, Collection<PointerKey>> scan = scanForMod(cg, pa, heapExclude);",
            "     return CallGraphTransitiveClosure.transitiveClosure(cg, scan);",
            "   }",
            "}")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "computeMod(com.ibm.wala.ipa.callgraph.CallGraph,com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis<T>,com.ibm.wala.ipa.slicer.HeapExclusions)",
                "heapExclude",
                "METHOD_PARAM",
                "com.uber.ModRef",
                "com.uber",
                "ModRef.java",
                "true"))
        .start();
  }

  @Test
  public void param_nullable_generics_multiple() {
    String rootName = "param_nullable_generics_multiple";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInput(
            "ModRef.java",
            "package com.uber;",
            "public class ModRef {",
            "   public ModRef(",
            "       IMethod method,",
            "       Context context,",
            "       AbstractCFG<?, ?> cfg,",
            "       SSAInstruction[] instructions,",
            "       SSAOptions options,",
            "       Map<Integer, ConstantValue> constants)",
            "       throws AssertionError {",
            "           super(",
            "               method, ",
            "               instructions,",
            "               makeSymbolTable(method, instructions, constants, cfg),",
            "               new SSACFG(method, cfg, instructions),",
            "               options",
            "           );",
            "         if (PARANOID) { repOK(instructions); }",
            "         setupLocationMap();",
            "    }",
            "}")
        .expectOutput(
            "ModRef.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class ModRef {",
            "   public ModRef(",
            "       IMethod method,",
            "       Context context,",
            "       AbstractCFG<?, ?> cfg,",
            "       SSAInstruction[] instructions,",
            "       SSAOptions options,",
            "       @Nullable() Map<Integer, ConstantValue> constants)",
            "       throws AssertionError {",
            "           super(",
            "               method, ",
            "               instructions,",
            "               makeSymbolTable(method, instructions, constants, cfg),",
            "               new SSACFG(method, cfg, instructions),",
            "               options",
            "           );",
            "         if (PARANOID) { repOK(instructions); }",
            "         setupLocationMap();",
            "    }",
            "}")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "ModRef(com.ibm.wala.classLoader.IMethod,com.ibm.wala.ipa.callgraph.Context,com.ibm.wala.cfg.AbstractCFG<?,?>,com.ibm.wala.ssa.SSAInstruction[],com.ibm.wala.ssa.SSAOptions,java.util.Map<java.lang.Integer,com.ibm.wala.ssa.ConstantValue>)",
                "constants",
                "METHOD_PARAM",
                "com.uber.ModRef",
                "com.uber",
                "ModRef.java",
                "true"))
        .start();
  }

  @Test
  public void param_nullable_with_annotation() {
    String rootName = "param_nullable_with_annotation";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInput(
            "WeakKeyReference.java",
            "package com.uber;",
            "class WeakKeyReference<K> extends WeakReference<K> implements InternalReference<K> {",
            "   private final int hashCode;",
            "   public WeakKeyReference(@Nullable() K key, ReferenceQueue<K> queue) {",
            "     super(key, queue);",
            "     hashCode = System.identityHashCode(key);",
            "   }",
            "}")
        .expectOutput(
            "WeakKeyReference.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "class WeakKeyReference<K> extends WeakReference<K> implements InternalReference<K> {",
            "   private final int hashCode;",
            "   public WeakKeyReference(@Nullable() K key, @Nullable() ReferenceQueue<K> queue) {",
            "     super(key, queue);",
            "     hashCode = System.identityHashCode(key);",
            "   }",
            "}")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "WeakKeyReference(@org.checkerframework.checker.nullness.qual.Nullable K,java.lang.ref.ReferenceQueue<K>)",
                "queue",
                "METHOD_PARAM",
                "com.uber.WeakKeyReference",
                "com.uber",
                "WeakKeyReference.java",
                "true"))
        .start();
  }

  @Test
  public void field_nullable_simple() {
    String rootName = "field_nullable_simple";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   Object h = new Object();",
            "   public void test(@Nullable() Object f) {",
            "      h = f;",
            "   }",
            "}")
        .expectOutput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   @Nullable() Object h = new Object();",
            "   public void test(@Nullable() Object f) {",
            "      h = f;",
            "   }",
            "}")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "",
                "h",
                "CLASS_FIELD",
                "com.uber.Super",
                "com.uber",
                "Super.java",
                "true"))
        .start();
  }

  @Test
  public void empty_method_param_pick() {
    String rootName = "empty_method_param_pick";

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   Object test() {",
            "       return new Object();",
            "   }",
            "   class SuperInner {",
            "       Object bar(@Nullable() Object foo) {",
            "           return foo;",
            "       }",
            "   }",
            "}")
        .expectOutput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Su  per {",
            "   @Nullable()",
            "   Object test() {",
            "       return new Object();",
            "   }",
            "   class SuperInner {",
            "       Object bar(@Nullable() Object foo) {",
            "           return foo;",
            "       }",
            "   }",
            "}")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "test()",
                "",
                "METHOD_RETURN",
                "com.uber.Super",
                "com.uber",
                "Super.java",
                "true"))
        .start();
  }

  @Test
  public void skip_duplicate_annotation() {
    String rootName = "skip_duplicate_annotation";

    Fix fix =
        new Fix(
            "javax.annotation.Nullable",
            "test()",
            "",
            "METHOD_RETURN",
            "com.uber.Super",
            "com.uber",
            "Super.java",
            "true");

    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   @Nullable() Object test() {",
            "       return new Object();",
            "   }",
            "}")
        .expectOutput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   @Nullable() Object test() {",
            "       return new Object();",
            "   }",
            "}")
        .addFixes(fix, fix.duplicate(), fix.duplicate())
        .start();
  }

  @Test
  public void save_imports() {
    String rootName = "save_imports";
    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInput(
            "Super.java",
            "package com.uber;",
            "import static com.ibm.wala.types.TypeName.ArrayMask;",
            "import static com.ibm.wala.types.TypeName.ElementBits;",
            "import static com.ibm.wala.types.TypeName.PrimitiveMask;",
            "import com.ibm.wala.types.TypeName.IntegerMask;",
            "import com.ibm.wala.util.collections.HashMapFactory;",
            "import java.io.Serializable;",
            "import java.util.Map;",
            "public class Super {",
            "   Object test() {",
            "       return new Object();",
            "   }",
            "}")
        .expectOutput(
            "Super.java",
            "package com.uber;",
            "import static com.ibm.wala.types.TypeName.ArrayMask;",
            "import static com.ibm.wala.types.TypeName.ElementBits;",
            "import static com.ibm.wala.types.TypeName.PrimitiveMask;",
            "import com.ibm.wala.types.TypeName.IntegerMask;",
            "import com.ibm.wala.util.collections.HashMapFactory;",
            "import java.io.Serializable;",
            "import java.util.Map;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   @Nullable() Object test() {",
            "       return new Object();",
            "   }",
            "}")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "test()",
                "",
                "METHOD_RETURN",
                "com.uber.Super",
                "com.uber",
                "Super.java",
                "true"))
        .start();
  }

  @Test
  public void remove_redundant_new_keyword() {
    String rootName = "remove_redundant_new_keyword";
    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInput(
            "Super.java",
            "package com.uber;",
            "public class Super {",
            "   Object test() {",
            "       init(this.new NodeVisitor(), this.new EdgeVisitor());\n",
            "       return foo(this.new Bar(), this.new Foo(), getBuilder().new Foo());",
            "   }",
            "   Object foo(Bar b, Foo f) {",
            "     return Object();",
            "   }",
            "   class Foo{ }",
            "   class Bar{ }",
            "}")
        .expectOutput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   @Nullable() Object test() {",
            "       init(this.new NodeVisitor(), this.new EdgeVisitor());",
            "       return foo(this.new Bar(), this.new Foo(), getBuilder().new Foo());",
            "   }",
            "   Object foo(Bar b, Foo f) {",
            "     return Object();",
            "   }",
            "   class Foo{ }",
            "   class Bar{ }",
            "}")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "test()",
                "",
                "METHOD_RETURN",
                "com.uber.Super",
                "com.uber",
                "Super.java",
                "true"))
        .start();
  }

  @Test
  public void skip_annotations_simple() {
    String rootName = "skip_annotations_simple";
    new InjectorTestHelper()
        .setRootPath(System.getProperty("user.dir") + "/tests/" + rootName)
        .addInput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   Object test(@javax.annotation.Nullable Object o) {",
            "   }",
            "}")
        .expectOutput(
            "Super.java",
            "package com.uber;",
            "import javax.annotation.Nullable;",
            "public class Super {",
            "   @Nullable() Object test(@javax.annotation.Nullable Object o) {",
            "   }",
            "}")
        .addFixes(
            new Fix(
                "javax.annotation.Nullable",
                "test(@javax.annotation.Nullable java.lang.Object)",
                "",
                "METHOD_RETURN",
                "com.uber.Super",
                "com.uber",
                "Super.java",
                "true"))
        .start();
  }
}

// todo: test these later:

//  For method pick (multiple Generics)
//  public <A extends AbstractValue<A>, S extends Store<S>, T extends TransferFunction<A, S>>
//  S test(TransferFunction<A, S> s, Context context, T transfer) {
//    return null;
//  }

// For method pick (For Generics as argument)
// ArrayList<String> vs ArrayList<Object>

//  For method pick (For Class<T> as argument
//  public static <T extends Shape> void drawWithShadow(T shape, Class<T> shapeClass) {
//    // The shadow must be the same shape as what's passed in
//    T shadow = shapeClass.newInstance();
//    // Set the shadow's properties to from the shape...
//    shadow.draw(); // First, draw the shadow
//    shape.draw();  // Now draw the shape on top of it
//  }
