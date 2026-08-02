package ch.so.agi.gretl.test.job;

import ch.so.agi.gretl.test.fixture.DuckDbExtensionsTestFixture;
import ch.so.agi.gretl.test.fixture.FtpTestFixture;
import ch.so.agi.gretl.test.fixture.PostgisTestFixture;
import ch.so.agi.gretl.test.fixture.RecordingHttpTestFixture;
import ch.so.agi.gretl.test.fixture.S3TestFixture;
import ch.so.agi.gretl.test.fixture.TestFixtureNetwork;
import ch.so.agi.gretl.test.fixture.TestFixtureOrchestrator;
import ch.so.agi.gretl.test.fixture.TestFixtureRegistry;

import java.util.List;
import java.util.Optional;

public final class TestJobExecutionSession implements AutoCloseable {
    private final TestJobExecutionSessionConfiguration configuration;
    private final TestFixtureNetwork network;
    private final TestFixtureOrchestrator fixtureOrchestrator;
    private final TestJobExecutionBackendFactory backendFactory;
    private final TestJobRunner runner;
    private boolean closed;

    private TestJobExecutionSession(TestJobExecutionSessionConfiguration configuration) {
        this.configuration = configuration;
        this.network = TestFixtureNetwork.create();
        this.fixtureOrchestrator = new TestFixtureOrchestrator(configuration.fixtureRegistry(), network);
        TestJobBackendContext context = withNetwork(configuration.backendContext(), network.dockerNetworkId(),
                configuration.materializedJobsRoot());
        this.backendFactory = new DefaultTestJobExecutionBackendFactory(context);
        this.runner = new TestJobRunner(configuration.materializer(), backendFactory,
                configuration.assertionRegistry(), context, fixtureOrchestrator);
    }

    public static TestJobExecutionSession open(TestJobExecutionSessionConfiguration configuration) {
        TestJobExecutionSession session = new TestJobExecutionSession(configuration);
        try {
            if (configuration.target() == TestJobExecutionTarget.RUNTIME_IMAGE_SERVICE) {
                session.backend();
            }
            return session;
        } catch (RuntimeException failure) {
            try { session.close(); } catch (RuntimeException cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
    }

    public TestJobExecutionTarget target() { return configuration.target(); }
    public java.nio.file.Path materializedJobsRoot() { return configuration.materializedJobsRoot(); }
    public TestJobRunner runner() { return runner; }
    public TestFixtureOrchestrator fixtures() { return fixtureOrchestrator; }
    public TestJobExecutionBackend backend() { return backendFactory.require(configuration.target()); }
    public TestFixtureNetwork network() { return network; }

    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;
        RuntimeException failure = null;
        try { backendFactory.close(); }
        catch (RuntimeException e) { failure = e; }
        try { fixtureOrchestrator.close(); }
        catch (RuntimeException e) { if (failure == null) failure = e; else failure.addSuppressed(e); }
        try { network.close(); }
        catch (RuntimeException e) { if (failure == null) failure = e; else failure.addSuppressed(e); }
        if (failure != null) throw failure;
    }

    public static TestFixtureRegistry defaultFixtureRegistry() {
        return new TestFixtureRegistry(List.of(new RecordingHttpTestFixture(), new FtpTestFixture(),
                new S3TestFixture(), new PostgisTestFixture(), new DuckDbExtensionsTestFixture()));
    }

    private static TestJobBackendContext withNetwork(TestJobBackendContext context, String network,
                                                      java.nio.file.Path jobsRoot) {
        Optional<java.nio.file.Path> serviceRoot = context.serviceJobsRoot().isPresent()
                ? context.serviceJobsRoot() : Optional.of(jobsRoot);
        Optional<java.nio.file.Path> serviceHome = context.serviceGradleHome().isPresent()
                ? context.serviceGradleHome() : Optional.of(jobsRoot.resolveSibling("service-gradle-home"));
        return new TestJobBackendContext(context.explicitPluginClasspathFile(), context.testKitDirectory(),
                context.publishedRepository(), context.pluginVersion(), context.runtimeImage(), serviceRoot,
                serviceHome, Optional.of(network), context.runtimeUser());
    }
}
