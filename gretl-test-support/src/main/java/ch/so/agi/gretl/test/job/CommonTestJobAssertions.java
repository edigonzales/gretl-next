package ch.so.agi.gretl.test.job;

import ch.so.agi.gretl.test.execution.GretlBuildResult;

import java.util.Set;

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
        String output = result.output();
        boolean remoteDownload = output.matches("(?is).*\\bdownloading\\b.*")
                || output.matches("(?is).*\\bdownloaded from\\b.*")
                || output.matches("(?is).*\\bcould not resolve .*\\bfrom\\b\\s+https?://.*")
                || output.matches("(?is).*\\bcould not (get|head) https?://.*");
        if (remoteDownload) throw new AssertionError("Remote download message found: " + output);
    }

    public static void assertSecretsAbsent(GretlBuildResult result, Set<String> secrets) {
        for (String secret : secrets) {
            if (secret != null && !secret.isEmpty() && result.output().contains(secret)) {
                throw new AssertionError("Secret value leaked into GRETL output: " + secret);
            }
        }
    }

    private CommonTestJobAssertions() { }
}
