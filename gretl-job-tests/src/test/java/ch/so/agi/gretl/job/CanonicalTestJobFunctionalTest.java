package ch.so.agi.gretl.job;

import ch.so.agi.gretl.test.job.DefaultTestJobMaterializer;
import ch.so.agi.gretl.test.job.FileSystemTestJobCatalog;
import ch.so.agi.gretl.test.job.TestJobAssertionRegistry;
import ch.so.agi.gretl.test.job.TestJobBackendContext;
import ch.so.agi.gretl.test.job.TestJobBuildVariant;
import ch.so.agi.gretl.test.job.TestJobDescriptor;
import ch.so.agi.gretl.test.job.TestJobExecutionCase;
import ch.so.agi.gretl.test.job.TestJobExecutionSelector;
import ch.so.agi.gretl.test.job.TestJobExecutionSession;
import ch.so.agi.gretl.test.job.TestJobExecutionSessionConfiguration;
import ch.so.agi.gretl.test.job.TestJobExecutionTarget;
import ch.so.agi.gretl.test.job.TestJobSelectionCriteria;
import ch.so.agi.gretl.test.job.TestJobSelectionProperties;
import ch.so.agi.gretl.test.job.TestJobRunRequest;
import ch.so.agi.gretl.test.job.TestJobRunResult;
import ch.so.agi.gretl.test.job.TestJobRunner;
import ch.so.agi.gretl.test.runtime.RuntimeImageDescriptor;
import ch.so.agi.gretl.test.trace.TaskExecutionTraceWriter;
import ch.so.agi.gretl.job.assertions.CanonicalTestJobAssertionRegistryFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CanonicalTestJobFunctionalTest {
    private static FileSystemTestJobCatalog catalog;
    private static TestJobExecutionTarget target;
    private static Path materializedRoot;
    private static TestJobRunner runner;
    private static TestJobExecutionSession session;

    @BeforeAll
    static void setUp() {
        catalog = FileSystemTestJobCatalog.load(requiredPath("gretl.test.jobsRoot"));
        target = TestJobExecutionTarget.valueOf(required("gretl.job.backend"));
        materializedRoot = requiredPath("gretl.test.materializedJobs");
        TestJobAssertionRegistry assertions = CanonicalTestJobAssertionRegistryFactory.create();
        session = TestJobExecutionSession.open(new TestJobExecutionSessionConfiguration(
                materializedRoot, target, backendContext(), new DefaultTestJobMaterializer(), assertions,
                TestJobExecutionSession.defaultFixtureRegistry(),
                ch.so.agi.gretl.test.job.MaterializedJobRetentionPolicy.KEEP_ALWAYS));
        runner = session.runner();
    }

    @AfterAll
    static void tearDown() {
        if (session != null) {
            session.close();
        }
    }

    @TestFactory
    Stream<DynamicTest> executesCanonicalJobs() {
        List<DynamicTest> tests = new ArrayList<>();
        TestJobSelectionCriteria criteria = TestJobSelectionProperties.fromSystemProperties(target);
        for (TestJobExecutionCase executionCase : new TestJobExecutionSelector().select(catalog, criteria)) {
            String name = target + " / " + executionCase.descriptor().id() + " / " + executionCase.buildVariant().id();
            tests.add(DynamicTest.dynamicTest(name, () -> run(executionCase.descriptor(), executionCase.buildVariant())));
        }
        return tests.stream();
    }

    private void run(TestJobDescriptor descriptor, TestJobBuildVariant build) throws Exception {
        TestJobRunResult run = runner.run(new TestJobRunRequest(
                descriptor, build, target, materializedRoot,
                java.util.Map.of(), java.util.Map.of(), Set.of(),
                Optional.ofNullable(System.getProperty("gretl.test.dockerNetwork")), true));
        assertEquals(0, run.buildResult().exitCode(), run.buildResult().output());
        writeCoverageTrace(run);
    }

    private void writeCoverageTrace(TestJobRunResult run) throws IOException {
        String root = System.getProperty("gretl.test.coverageTraceRoot");
        if (root == null || root.isBlank()) return;
        Path traceFile = Path.of(root).toAbsolutePath().normalize()
                .resolve(target.name().toLowerCase())
                .resolve(run.job().descriptor().id())
                .resolve(run.job().buildVariant().id() + ".jsonl");
        new TaskExecutionTraceWriter().write(traceFile, run.trace());
    }

    private static TestJobBackendContext backendContext() {
        Optional<Path> explicitClasspath = optionalPath("gretl.test.explicitPluginClasspath");
        Optional<Path> testKitDirectory = optionalPath("gretl.test.testKitDirectory");
        Optional<URI> publishedRepository = optionalPath("gretl.test.publishedRepository")
                .map(path -> path.toAbsolutePath().normalize().toUri());
        Optional<RuntimeImageDescriptor> image = isRuntimeTarget()
                ? Optional.of(RuntimeImageDescriptor.fromSystemProperties()) : Optional.empty();
        Optional<Path> serviceRoot = target == TestJobExecutionTarget.RUNTIME_IMAGE_SERVICE
                ? Optional.of(materializedRoot) : Optional.empty();
        Optional<Path> serviceHome = target == TestJobExecutionTarget.RUNTIME_IMAGE_SERVICE
                ? Optional.of(materializedRoot.resolveSibling("service-gradle-home")) : Optional.empty();
        return new TestJobBackendContext(explicitClasspath, testKitDirectory,
                publishedRepository, optional("gretl.test.pluginVersion"), image,
                serviceRoot, serviceHome, optional("gretl.test.dockerNetwork"),
                optional("gretl.test.runtimeImage.user"));
    }

    private static boolean isRuntimeTarget() {
        return target == TestJobExecutionTarget.RUNTIME_IMAGE_ONE_SHOT
                || target == TestJobExecutionTarget.RUNTIME_IMAGE_SERVICE;
    }

    private static Optional<Path> optionalPath(String property) {
        return optional(property).map(Path::of);
    }

    private static Optional<String> optional(String property) {
        return Optional.ofNullable(System.getProperty(property)).filter(value -> !value.isBlank());
    }

    private static String required(String property) {
        return optional(property).orElseThrow(() -> new IllegalStateException("Missing " + property));
    }

    private static Path requiredPath(String property) {
        return Path.of(required(property)).toAbsolutePath().normalize();
    }

}
