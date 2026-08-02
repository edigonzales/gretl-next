package ch.so.agi.gretl.job;

import ch.so.agi.gretl.test.execution.GretlBuildResult;
import ch.so.agi.gretl.test.job.CommonTestJobAssertions;
import ch.so.agi.gretl.test.job.DefaultTestJobMaterializer;
import ch.so.agi.gretl.test.job.FileSystemTestJobCatalog;
import ch.so.agi.gretl.test.job.MaterializedTestJob;
import ch.so.agi.gretl.test.job.TestJobAssertionRegistry;
import ch.so.agi.gretl.test.job.TestJobAssertions;
import ch.so.agi.gretl.test.job.TestJobBackendContext;
import ch.so.agi.gretl.test.job.TestJobBuildVariant;
import ch.so.agi.gretl.test.job.TestJobDescriptor;
import ch.so.agi.gretl.test.job.TestJobExecutionRequirement;
import ch.so.agi.gretl.test.job.TestJobExecutionCase;
import ch.so.agi.gretl.test.job.TestJobExecutionSelector;
import ch.so.agi.gretl.test.job.TestJobExecutionSession;
import ch.so.agi.gretl.test.job.TestJobExecutionSessionConfiguration;
import ch.so.agi.gretl.test.job.TestJobExecutionTarget;
import ch.so.agi.gretl.test.job.TestJobRunRequest;
import ch.so.agi.gretl.test.job.TestJobRunResult;
import ch.so.agi.gretl.test.job.TestJobRunner;
import ch.so.agi.gretl.test.runtime.RuntimeImageDescriptor;
import ch.so.agi.gretl.test.trace.TaskExecutionTrace;
import ch.so.agi.gretl.test.trace.TaskExecutionTraceWriter;
import org.geotools.api.parameter.GeneralParameterValue;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.referencing.CRS;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        TestJobAssertionRegistry assertions = new TestJobAssertionRegistry(List.of(
                        new GzipAssertions(), new SqliteAssertions(),
                        new CombinedRasterAssertions(), new ReadShapefileAssertions(),
                        new P2CanonicalAssertions("core-duckdb-spatial"),
                        new P2CanonicalAssertions("network-http-curl"),
                        new P2CanonicalAssertions("network-ftp-roundtrip"),
                        new P2CanonicalAssertions("network-s3-roundtrip"),
                        new P2CanonicalAssertions("database-postgis-sql"),
                        new P2CanonicalAssertions("interlis-ili2duckdb-roundtrip"),
                        new P2CanonicalAssertions("interlis-ili2pg-lifecycle")));
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
        boolean includeOptional = Boolean.getBoolean("gretl.job.includeOptional");
        Set<String> categoryFilter = java.util.Arrays.stream(
                        System.getProperty("gretl.job.categories", "").split(","))
                .map(String::trim).filter(value -> !value.isEmpty()).collect(java.util.stream.Collectors.toSet());
        List<DynamicTest> tests = new ArrayList<>();
        for (TestJobExecutionCase executionCase : new TestJobExecutionSelector().select(catalog, target, includeOptional)) {
            if (!categoryFilter.isEmpty() && !categoryFilter.contains(executionCase.descriptor().category())) {
                continue;
            }
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

    private abstract static class CommonAssertions implements TestJobAssertions {
        @Override
        public final void verify(MaterializedTestJob job, GretlBuildResult result, TaskExecutionTrace trace) throws Exception {
            CommonTestJobAssertions.assertSuccessful(result);
            CommonTestJobAssertions.assertNoClassloaderFailure(result);
            if (job.target() == TestJobExecutionTarget.RUNTIME_IMAGE_ONE_SHOT
                    || job.target() == TestJobExecutionTarget.RUNTIME_IMAGE_SERVICE) {
                CommonTestJobAssertions.assertNoRemoteDownloadLog(result);
            }
            verifyJob(job, result, trace);
        }

        protected abstract void verifyJob(MaterializedTestJob job, GretlBuildResult result,
                                          TaskExecutionTrace trace) throws Exception;
    }

    private static final class GzipAssertions extends CommonAssertions {
        @Override public String id() { return "core-gzip"; }

        @Override
        protected void verifyJob(MaterializedTestJob job, GretlBuildResult result, TaskExecutionTrace trace) throws Exception {
            Path output = job.resolve("build/output/data.txt.gz");
            assertTrue(Files.isRegularFile(output), "GZIP output missing: " + output);
            byte[] actual;
            try (InputStream input = new GZIPInputStream(Files.newInputStream(output))) {
                actual = input.readAllBytes();
            }
            assertArrayEquals(Files.readAllBytes(job.resolveExpected("payload.txt")), actual);
        }
    }

    private static final class SqliteAssertions extends CommonAssertions {
        @Override public String id() { return "core-sqlite"; }

        @Override
        protected void verifyJob(MaterializedTestJob job, GretlBuildResult result, TaskExecutionTrace trace) throws Exception {
            Path database = job.resolve("build/db/test.db");
            assertTrue(Files.isRegularFile(database), "SQLite database missing: " + database);
            try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database);
                 Statement statement = connection.createStatement();
                 ResultSet rows = statement.executeQuery("select id, label from canonical_values order by id")) {
                assertTrue(rows.next());
                assertEquals(1, rows.getInt("id"));
                assertEquals("alpha", rows.getString("label"));
                assertTrue(rows.next());
                assertEquals(2, rows.getInt("id"));
                assertEquals("beta", rows.getString("label"));
                assertFalse(rows.next());
            }
        }
    }

    private static final class CombinedRasterAssertions extends CommonAssertions {
        @Override public String id() { return "combined-core-geotools-pipeline"; }

        @Override
        protected void verifyJob(MaterializedTestJob job, GretlBuildResult result, TaskExecutionTrace trace) throws Exception {
            Path ascii = job.resolve("build/generated/raster.asc");
            Path geotiff = job.resolve("build/geotools/reclassified.tif");
            Path gzip = job.resolve("build/distribution/reclassified.tif.gz");
            assertTrue(Files.isRegularFile(ascii));
            assertTrue(Files.isRegularFile(geotiff));
            assertTrue(Files.isRegularFile(gzip));
            assertArrayEquals(Files.readAllBytes(geotiff), gunzip(gzip));
            List<String> lines = Files.readAllLines(ascii, StandardCharsets.UTF_8);
            assertEquals("ncols 4", lines.get(0).trim());
            assertEquals("nrows 3", lines.get(1).trim());
            assertEquals("10 56 61 71", lines.get(6).trim());
            assertEquals("-9999 0 70 500", lines.get(8).trim());
            assertRaster(geotiff);
        }

        private static byte[] gunzip(Path file) throws IOException {
            try (GZIPInputStream input = new GZIPInputStream(Files.newInputStream(file))) {
                return input.readAllBytes();
            }
        }

        private static void assertRaster(Path file) throws Exception {
            AbstractGridFormat format = GridFormatFinder.findFormat(file.toFile());
            assertNotNull(format);
            GridCoverage2DReader reader = format.getReader(file.toFile());
            try {
                GridCoverage2D coverage = reader.read((GeneralParameterValue[]) null);
                assertEquals(4, coverage.getRenderedImage().getWidth());
                assertEquals(3, coverage.getRenderedImage().getHeight());
                CoordinateReferenceSystem crs = coverage.getCoordinateReferenceSystem2D();
                assertEquals("EPSG:2056", CRS.lookupIdentifier(crs, true));
                double[][] expected = {{0, 55, 60, 70}, {0, 55, 60, 70}, {-100, 0, 70, 70}};
                var raster = coverage.getRenderedImage().getData();
                for (int row = 0; row < expected.length; row++) {
                    for (int column = 0; column < expected[row].length; column++) {
                        assertEquals(expected[row][column], raster.getSampleDouble(column, row, 0), 0.000001);
                    }
                }
            } finally {
                reader.dispose();
            }
        }
    }

    private static final class ReadShapefileAssertions extends CommonAssertions {
        @Override public String id() { return "geotools-read-shapefile"; }

        @Override
        protected void verifyJob(MaterializedTestJob job, GretlBuildResult result, TaskExecutionTrace trace) throws Exception {
            assertTrue(result.output().contains("Feature count:"), result.output());
            assertTrue(result.output().contains("Target CRS: EPSG:2056"), result.output());
            assertFalse(result.output().contains("GRETL_WORKER|"), result.output());
            assertTrue(Files.isRegularFile(job.resolveExpected("features.json")));
        }
    }
}
