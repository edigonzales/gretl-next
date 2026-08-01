package ch.so.agi.gretl.test.execution;

import java.nio.file.Path;

public interface GradleUserHomeHandle extends AutoCloseable {
    Path path();

    @Override
    void close();
}
