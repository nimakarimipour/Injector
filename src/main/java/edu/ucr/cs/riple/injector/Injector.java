package edu.ucr.cs.riple.injector;

import java.util.List;

@SuppressWarnings(
        "UnusedVariable") // todo: Remove this later, this class is still under construction
public class Injector {
  public final MODE mode;

  public enum MODE {
    BATCH,
    TEST
  }

  public Injector(MODE mode) {
    this.mode = mode;
  }

  public static InjectorBuilder builder() {
    return new InjectorBuilder();
  }

  public Report start(List<WorkList> workLists) {
    Report report = new Report();
    for (WorkList workList : workLists)
      report.totalNumberOfDistinctFixes += workList.getFixes().size();
    System.out.println("Received " + report.totalNumberOfDistinctFixes + " number of fixes");
    report.processed = new InjectorMachine(workLists, mode).start();
    System.out.println(
            "Received "
                    + report.totalNumberOfDistinctFixes
                    + " fixes and applied "
                    + report.processed
                    + " number of fixes");

    return report;
  }

  public static class InjectorBuilder {
    private MODE mode = MODE.BATCH;

    public InjectorBuilder setMode(MODE mode){
      this.mode = mode;
      return this;
    }

    public Injector build() {
      return new Injector(mode);
    }
  }
}
