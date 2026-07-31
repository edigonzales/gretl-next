package ch.so.agi.gretl.test.execution;

public interface GretlBuildExecutor {
    GretlBuildResult execute(GretlBuildRequest request);

    GretlBuildResult executeAndExpectFailure(GretlBuildRequest request);
}
