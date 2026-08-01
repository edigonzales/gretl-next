package ch.so.agi.gretl.test.execution;

import java.nio.file.Path;

public interface GradleUserHomeStrategy {
    GradleUserHomeHandle prepare(Path projectDirectory, RuntimeExecutionMode executionMode);
}
