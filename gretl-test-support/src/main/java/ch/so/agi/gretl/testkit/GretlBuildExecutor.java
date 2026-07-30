package ch.so.agi.gretl.testkit;

import org.gradle.testkit.runner.BuildResult;

import java.nio.file.Path;

public interface GretlBuildExecutor {
    BuildResult run(Path projectDirectory, String... arguments);

    BuildResult runAndFail(Path projectDirectory, String... arguments);
}
