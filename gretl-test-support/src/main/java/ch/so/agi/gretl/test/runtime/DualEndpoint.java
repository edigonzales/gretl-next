package ch.so.agi.gretl.test.runtime;

import ch.so.agi.gretl.test.execution.GretlExecutionMode;

public record DualEndpoint(String hostValue, String containerValue) {
    public String forMode(GretlExecutionMode mode) {
        return mode == GretlExecutionMode.RUNTIME_IMAGE ? containerValue : hostValue;
    }
}
