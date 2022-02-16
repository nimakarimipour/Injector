package edu.ucr.cs.riple.injector;

public class Main {

  public static void main(String[] args) {
    Injector.LOG = false;
    String path = args[0];
    System.out.println("Injector reading fixes at: " + path);
    Injector injector =
        new Injector.InjectorBuilder().setMode(Injector.MODE.BATCH).keepStyle(true).build();
    injector.start(new WorkListBuilder(path).getWorkLists());
  }
}
