package ch.so.agi.gretl.test.job;

import ch.so.agi.gretl.test.execution.GretlBuildResult;

public final class CommonTestJobAssertions {
    public static void assertSuccessful(GretlBuildResult result) {
        if (!result.successful()) throw new AssertionError("GRETL build failed: " + result.output());
    }

    public static void assertNoClassloaderFailure(GretlBuildResult result) {
        String output = result.output();
        if (output.contains("ClassNotFoundException") || output.contains("NoClassDefFoundError")) throw new AssertionError(output);
    }

    public static void assertNoWorkerProtocolLeak(GretlBuildResult result) {
        if (result.output().contains("GRETL_WORKER|")) throw new AssertionError("Worker protocol leaked into output: " + result.output());
    }

    public static void assertNoRemoteDownloadLog(GretlBuildResult result) {
        if (result.output().matches("(?is).*\\b(download|downloading|downloading)\\b.*")) throw new AssertionError("Remote download message found: " + result.output());
    }

    private CommonTestJobAssertions() { }
}
