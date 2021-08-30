package edu.ucr.cs.riple.injector;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Main {

  private static Injector injector;

  public static void main(String[] args) {
    injector = Injector.builder().setMode(Injector.MODE.BATCH).build();
    Fix fix =
        new Fix(
            "javax.annotation.Nullable",
            "null",
            "padTop",
            "CLASS_FIELD",
            "com.badlogic.gdx.scenes.scene2d.ui.Cell",
            "//Users/nima/Developer/NullAwayFixer/Projects/libgdx/gdx/src/com/badlogic/gdx/scenes/scene2d/ui/Cell.java",
            "true");
    fix.index = "0";
    apply(Collections.singletonList(fix));
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
