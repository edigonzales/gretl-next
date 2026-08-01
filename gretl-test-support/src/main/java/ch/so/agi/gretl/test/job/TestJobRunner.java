package ch.so.agi.gretl.test.job;

import ch.so.agi.gretl.test.trace.ExpectedTaskTraceVerifier;
import ch.so.agi.gretl.test.trace.TaskExecutionTrace;
import ch.so.agi.gretl.test.trace.TaskExecutionTraceReader;

import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

public final class TestJobRunner {
    private final TestJobMaterializer materializer;
    private final TestJobExecutionBackendFactory backendFactory;
    private final TestJobAssertionRegistry assertionRegistry;
    private final TaskExecutionTraceReader traceReader;
    private final ExpectedTaskTraceVerifier traceVerifier;
    private final TestJobDescriptorValidator validator;
    private final TestJobBackendContext backendContext;

    public TestJobRunner(TestJobMaterializer materializer, TestJobExecutionBackendFactory backendFactory,
                         TestJobAssertionRegistry assertionRegistry) {
        this(materializer, backendFactory, assertionRegistry, new TestJobBackendContext(
                java.util.Optional.empty(), java.util.Optional.empty(), java.util.Optional.empty(), java.util.Optional.empty(),
                java.util.Optional.empty(), java.util.Optional.empty(), java.util.Optional.empty(), java.util.Optional.empty(), java.util.Optional.empty()));
    }

    public TestJobRunner(TestJobMaterializer materializer, TestJobExecutionBackendFactory backendFactory,
                         TestJobAssertionRegistry assertionRegistry, TestJobBackendContext backendContext) {
        this.materializer = materializer;
        this.backendFactory = backendFactory;
        this.assertionRegistry = assertionRegistry;
        this.backendContext = backendContext;
        this.traceReader = new TaskExecutionTraceReader();
        this.traceVerifier = new ExpectedTaskTraceVerifier();
        this.validator = new TestJobDescriptorValidator();
    }

    public TestJobRunResult run(TestJobRunRequest request) throws Exception {
        validator.validate(request.descriptor());
        if (request.descriptor().requirementFor(request.target()) == TestJobExecutionRequirement.NOT_APPLICABLE) {
            throw new IllegalArgumentException("Job " + request.descriptor().id() + " is not applicable to " + request.target());
        }
        MaterializedTestJob job = materializer.materialize(request.descriptor(), request.buildVariant(), request.target(), request.destinationRoot());
        ArrayList<String> arguments = new ArrayList<>();
        if (request.traceEnabled()) {
            arguments.add("--init-script");
            arguments.add(job.traceBootstrapFile().toString());
            arguments.add("-Dgretl.test.traceEnabled=true");
            arguments.add("-Dgretl.test.traceFile=" + job.traceFile());
            arguments.add("-Dgretl.test.jobId=" + job.descriptor().id());
            arguments.add("-Dgretl.test.buildVariant=" + job.buildVariant().id());
            arguments.add("-Dgretl.test.executionTarget=" + job.target().name());
        }
        TestJobExecutionRequest executionRequest = new TestJobExecutionRequest(job, arguments,
                request.environment(), request.gradleProperties(), request.secrets(),
                request.descriptor().timeout(), request.dockerNetwork());
        TestJobExecutionBackend backend = backendFactory.create(request.target(), backendContext);
        ch.so.agi.gretl.test.execution.GretlBuildResult result = backend.execute(executionRequest);
        CommonTestJobAssertions.assertSuccessful(result);
        TaskExecutionTrace trace = request.traceEnabled() ? traceReader.read(job.traceFile()) : new TaskExecutionTrace(java.util.List.of());
        if (request.traceEnabled()) traceVerifier.verify(job.descriptor(), trace);
        assertionRegistry.require(job.descriptor().assertions()).verify(job, result, trace);
        return new TestJobRunResult(job, result, trace);
    }
}
