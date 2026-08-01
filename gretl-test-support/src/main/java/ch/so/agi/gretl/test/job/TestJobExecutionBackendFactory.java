package ch.so.agi.gretl.test.job;

public interface TestJobExecutionBackendFactory extends AutoCloseable {
    TestJobExecutionBackend create(TestJobExecutionTarget target, TestJobBackendContext context);
    @Override default void close() { }
}
