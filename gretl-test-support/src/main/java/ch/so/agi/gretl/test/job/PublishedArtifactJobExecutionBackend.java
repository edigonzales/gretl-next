package ch.so.agi.gretl.test.job;

public final class PublishedArtifactJobExecutionBackend extends TestKitJobExecutionBackend {
    public PublishedArtifactJobExecutionBackend(ch.so.agi.gretl.testkit.GretlBuildExecutor delegate) {
        super(delegate, TestJobExecutionTarget.PUBLISHED_ARTIFACT);
    }
}
