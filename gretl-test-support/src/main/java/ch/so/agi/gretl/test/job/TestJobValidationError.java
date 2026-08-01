package ch.so.agi.gretl.test.job;

public record TestJobValidationError(String field, String message) {
    @Override
    public String toString() {
        return field + ": " + message;
    }
}
