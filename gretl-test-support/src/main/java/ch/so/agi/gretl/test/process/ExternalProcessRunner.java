package ch.so.agi.gretl.test.process;

/** Named facade used by black-box tests; the existing ProcessExecutor remains the shared implementation. */
public final class ExternalProcessRunner {
    private final ProcessExecutor delegate;

    public ExternalProcessRunner() {
        this.delegate = new ProcessExecutor();
    }

    public ExternalProcessRunner(SecretRedactor redactor) {
        this.delegate = new ProcessExecutor(redactor);
    }

    public ProcessResult execute(ProcessRequest request) {
        return delegate.execute(request);
    }
}
