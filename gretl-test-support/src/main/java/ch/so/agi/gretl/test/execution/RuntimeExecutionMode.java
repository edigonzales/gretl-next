package ch.so.agi.gretl.test.execution;

/**
 * Describes the container and Gradle lifecycle, independently of dependency
 * policy and Docker networking.
 */
public enum RuntimeExecutionMode {
    ONE_SHOT,
    SERVICE
}
