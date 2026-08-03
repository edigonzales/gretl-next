package ch.so.agi.gretl.test.job;

import ch.so.agi.gretl.test.trace.ExpectedTaskTraceVerifier;
import ch.so.agi.gretl.test.trace.TaskExecutionTrace;
import ch.so.agi.gretl.test.trace.TaskExecutionTraceReader;
import ch.so.agi.gretl.test.fixture.PreparedTestJobEnvironment;
import ch.so.agi.gretl.test.fixture.TestFixtureOrchestrator;
import ch.so.agi.gretl.test.fixture.TestJobEnvironment;

import java.util.ArrayList;

public final class TestJobRunner {
    private final TestJobMaterializer materializer;
    private final TestJobExecutionBackendFactory backendFactory;
    private final TestJobAssertionRegistry assertionRegistry;
    private final TaskExecutionTraceReader traceReader;
    private final ExpectedTaskTraceVerifier traceVerifier;
    private final TestJobDescriptorValidator validator;
    private final TestJobBackendContext backendContext;
    private final TestFixtureOrchestrator fixtureOrchestrator;

    public TestJobRunner(TestJobMaterializer materializer, TestJobExecutionBackendFactory backendFactory,
                         TestJobAssertionRegistry assertionRegistry) {
        this(materializer, backendFactory, assertionRegistry, new TestJobBackendContext(
                java.util.Optional.empty(), java.util.Optional.empty(), java.util.Optional.empty(), java.util.Optional.empty(),
                java.util.Optional.empty(), java.util.Optional.empty(), java.util.Optional.empty(), java.util.Optional.empty(), java.util.Optional.empty()));
    }

    public TestJobRunner(TestJobMaterializer materializer, TestJobExecutionBackendFactory backendFactory,
                         TestJobAssertionRegistry assertionRegistry, TestJobBackendContext backendContext) {
        this(materializer, backendFactory, assertionRegistry, backendContext, null);
    }

    public TestJobRunner(TestJobMaterializer materializer, TestJobExecutionBackendFactory backendFactory,
                         TestJobAssertionRegistry assertionRegistry, TestJobBackendContext backendContext,
                         TestFixtureOrchestrator fixtureOrchestrator) {
        this.materializer = materializer;
        this.backendFactory = backendFactory;
        this.assertionRegistry = assertionRegistry;
        this.backendContext = backendContext;
        this.traceReader = new TaskExecutionTraceReader();
        this.traceVerifier = new ExpectedTaskTraceVerifier();
        this.validator = new TestJobDescriptorValidator();
        this.fixtureOrchestrator = fixtureOrchestrator;
    }

    public TestJobRunResult run(TestJobRunRequest request) throws Exception {
        MaterializedTestJob job = null;
        PreparedTestJobEnvironment prepared = null;
        boolean successful = false;
        TestJobRunResult runResult = null;
        Throwable primaryFailure = null;
        try {
            validator.validate(request.descriptor());
            if (request.descriptor().requirementFor(request.target()) == TestJobExecutionRequirement.NOT_APPLICABLE) {
                throw new IllegalArgumentException("Job " + request.descriptor().id() + " is not applicable to " + request.target());
            }
            job = materializer.materialize(request.descriptor(), request.buildVariant(), request.target(),
                    request.destinationRoot(), request.executionId());
            prepared = fixtureOrchestrator == null
                    ? new PreparedTestJobEnvironment(TestJobEnvironment.empty(), java.util.Map.of())
                    : fixtureOrchestrator.prepare(request.descriptor(), request.buildVariant(), request.target(),
                            request.executionId());
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
            TestJobEnvironment environment = prepared.environment().merge(request.callerOverrides());
            TestJobExecutionRequest executionRequest = new TestJobExecutionRequest(job, arguments,
                    environment.environmentVariables(), environment.gradleProperties(), environment.secretValues(),
                    request.descriptor().timeout(), environment.dockerNetwork());
            if (backendContext != null && backendFactory instanceof DefaultTestJobExecutionBackendFactory) {
                backendFactory.create(request.target(), backendContext);
            }
            TestJobExecutionBackend backend = backendFactory.require(request.target());
            ch.so.agi.gretl.test.execution.GretlBuildResult result = backend.execute(executionRequest);
            CommonTestJobAssertions.assertSuccessful(result);
            TaskExecutionTrace trace = request.traceEnabled() ? traceReader.read(job.traceFile())
                    : new TaskExecutionTrace(java.util.List.of());
            if (request.traceEnabled()) traceVerifier.verify(job.descriptor(), trace);
            assertionRegistry.require(job.descriptor().assertions()).verify(
                    new TestJobVerificationContext(job, result, trace, prepared));
            successful = true;
            runResult = new TestJobRunResult(job, result, trace);
        } catch (Throwable failure) {
            primaryFailure = failure;
        }
        RuntimeException cleanupFailure = new TestJobExecutionCleanup().cleanup(job, prepared,
                request.retentionPolicy(), successful);
        if (cleanupFailure != null) {
            if (primaryFailure == null) primaryFailure = cleanupFailure;
            else primaryFailure.addSuppressed(cleanupFailure);
        }
        if (primaryFailure != null) rethrow(primaryFailure);
        return runResult;
    }

    private static void rethrow(Throwable failure) throws Exception {
        if (failure instanceof Error error) throw error;
        if (failure instanceof Exception exception) throw exception;
        throw new IllegalStateException(failure);
    }
}
