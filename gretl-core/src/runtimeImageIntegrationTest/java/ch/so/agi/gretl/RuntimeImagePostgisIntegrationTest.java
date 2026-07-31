package ch.so.agi.gretl;

import ch.so.agi.gretl.test.docker.ContainerUserResolver;
import ch.so.agi.gretl.test.docker.DockerCli;
import ch.so.agi.gretl.test.execution.GretlBuildRequest;
import ch.so.agi.gretl.test.execution.GretlBuildResult;
import ch.so.agi.gretl.test.execution.RuntimeImageBuildExecutor;
import ch.so.agi.gretl.test.execution.RuntimeImageGradleArguments;
import ch.so.agi.gretl.test.runtime.RuntimeImageDescriptor;
import ch.so.agi.gretl.test.runtime.RuntimeImageRunOptions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("runtime-image")
@Tag("runtime-image-smoke")
@Tag("requires-postgis")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RuntimeImagePostgisIntegrationTest {
    private static final Network NETWORK = Network.newNetwork();
    private static final PostgreSQLContainer<?> POSTGIS = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("gretl")
            .withUsername("gretl")
            .withPassword("gretl")
            .withNetwork(NETWORK)
            .withNetworkAliases("postgis");

    @TempDir
    Path project;

    @Test
    void writesSqlThroughContainerNetworkAndCanBeReadFromHost() throws Exception {
        POSTGIS.start();
        try {
            try (var connection = DriverManager.getConnection(POSTGIS.getJdbcUrl(), POSTGIS.getUsername(), POSTGIS.getPassword());
                 var statement = connection.createStatement()) {
                statement.execute("create table colors (id integer primary key, name text)");
            }
            Files.writeString(project.resolve("settings.gradle"), "rootProject.name = 'runtime-postgis'\n");
            Files.writeString(project.resolve("fill.sql"), "insert into colors values (1, 'runtime')", StandardCharsets.UTF_8);
            Files.writeString(project.resolve("build.gradle"), """
                    plugins { id 'ch.so.agi.gretl' }
                    import ch.so.agi.gretl.tasks.SqlExecutor
                    tasks.register('fill', SqlExecutor) {
                        database providers.gradleProperty('pgUrl').get(), providers.gradleProperty('pgUser').get(), providers.gradleProperty('pgPass').get()
                        sqlFiles 'fill.sql'
                    }
                    """, StandardCharsets.UTF_8);

            RuntimeImageDescriptor image = RuntimeImageDescriptor.fromSystemProperties();
            RuntimeImageBuildExecutor executor = new RuntimeImageBuildExecutor(image, new DockerCli(),
                    new ContainerUserResolver(), new RuntimeImageGradleArguments());
            GretlBuildRequest request = GretlBuildRequest.builder(project)
                    .arguments("--no-daemon", "--offline", "--rerun-tasks", "-PpgUrl=jdbc:postgresql://postgis:5432/gretl",
                            "-PpgUser=gretl", "-PpgPass=gretl", "fill")
                    .secret("gretl")
                    .timeout(Duration.ofMinutes(10))
                    .runtimeImageOptions(RuntimeImageRunOptions.onNetwork(NETWORK.getId()))
                    .build();
            GretlBuildResult result = executor.execute(request);
            assertTrue(result.successful(), result.output());
            try (var connection = DriverManager.getConnection(POSTGIS.getJdbcUrl(), POSTGIS.getUsername(), POSTGIS.getPassword());
                 var statement = connection.createStatement();
                 var rows = statement.executeQuery("select name from colors")) {
                assertTrue(rows.next());
                assertEquals("runtime", rows.getString(1));
            }
        } finally {
            POSTGIS.stop();
        }
    }

    @Test
    void runsIli2pgTaskChainThroughRuntimeImage() throws Exception {
        POSTGIS.start();
        try {
            try (var connection = DriverManager.getConnection(POSTGIS.getJdbcUrl(), POSTGIS.getUsername(), POSTGIS.getPassword());
                 var statement = connection.createStatement()) {
                statement.execute("create schema ili2pgtest");
            }
            copyResource("fixtures/interlis/ili2db/beispiel2/Beispiel2.ili", "Beispiel2.ili");
            copyResource("fixtures/interlis/ili2db/beispiel2/Beispiel2a.xtf", "Beispiel2a.xtf");
            copyResource("fixtures/interlis/ili2db/beispiel2/Beispiel2b.xtf", "Beispiel2b.xtf");
            Files.writeString(project.resolve("settings.gradle"), "rootProject.name = 'runtime-ili2pg'\n");
            Files.writeString(project.resolve("build.gradle"), """
                    plugins { id 'ch.so.agi.gretl' }

                    import ch.so.agi.gretl.tasks.Ili2pgImportSchema
                    import ch.so.agi.gretl.tasks.Ili2pgImport
                    import ch.so.agi.gretl.tasks.Ili2pgReplace
                    import ch.so.agi.gretl.tasks.Ili2pgUpdate
                    import ch.so.agi.gretl.tasks.Ili2pgValidate
                    import ch.so.agi.gretl.tasks.Ili2pgExport
                    import ch.so.agi.gretl.tasks.Ili2pgDelete

                    def pgUrl = providers.gradleProperty('pgUrl').get()
                    def pgUser = providers.gradleProperty('pgUser').get()
                    def pgPass = providers.gradleProperty('pgPass').get()

                    tasks.register('schema', Ili2pgImportSchema) {
                        database pgUrl, pgUser, pgPass
                        models 'Beispiel2'; modeldir projectDir.toString(); dbschema 'ili2pgtest'
                        defaultSrsCode '2056'; createBasketCol true; createTidCol true
                    }
                    tasks.register('importData', Ili2pgImport) {
                        dependsOn 'schema'
                        database pgUrl, pgUser, pgPass; dbschema 'ili2pgtest'
                        transferFiles 'Beispiel2a.xtf', 'Beispiel2b.xtf'
                        dataset(['DatasetA', 'DatasetB']); importTid true
                    }
                    tasks.register('replaceA', Ili2pgReplace) {
                        dependsOn 'importData'
                        database pgUrl, pgUser, pgPass; dbschema 'ili2pgtest'
                        transferFiles 'Beispiel2a.xtf'; dataset 'DatasetA'; importTid true
                    }
                    tasks.register('updateA', Ili2pgUpdate) {
                        dependsOn 'replaceA'
                        database pgUrl, pgUser, pgPass; dbschema 'ili2pgtest'
                        transferFiles 'Beispiel2a.xtf'; dataset 'DatasetA'; importTid true
                    }
                    tasks.register('validateData', Ili2pgValidate) {
                        dependsOn 'updateA'
                        database pgUrl, pgUser, pgPass; models 'Beispiel2'
                        modeldir projectDir.toString(); dbschema 'ili2pgtest'; dataset 'DatasetA'
                        logFile layout.buildDirectory.file('validation.log')
                    }
                    tasks.register('exportData', Ili2pgExport) {
                        dependsOn 'validateData'
                        database pgUrl, pgUser, pgPass; models 'Beispiel2'
                        modeldir projectDir.toString(); dbschema 'ili2pgtest'
                        dataFiles layout.buildDirectory.file('DatasetA-out.xtf'); dataset 'DatasetA'
                    }
                    tasks.register('deleteData', Ili2pgDelete) {
                        dependsOn 'exportData'
                        database pgUrl, pgUser, pgPass; dbschema 'ili2pgtest'
                        dataset(['DatasetA', 'DatasetB'])
                    }
                    """, StandardCharsets.UTF_8);

            RuntimeImageBuildExecutor executor = new RuntimeImageBuildExecutor(
                    RuntimeImageDescriptor.fromSystemProperties(), new DockerCli(),
                    new ContainerUserResolver(), new RuntimeImageGradleArguments());
            GretlBuildResult result = executor.execute(GretlBuildRequest.builder(project)
                    .arguments("--no-daemon", "--offline", "--rerun-tasks",
                            "-PpgUrl=jdbc:postgresql://postgis:5432/gretl",
                            "-PpgUser=" + POSTGIS.getUsername(), "-PpgPass=" + POSTGIS.getPassword(), "deleteData")
                    .secret(POSTGIS.getPassword())
                    .timeout(Duration.ofMinutes(15))
                    .runtimeImageOptions(RuntimeImageRunOptions.onNetwork(NETWORK.getId()))
                    .build());

            assertTrue(result.successful(), result.output());
            assertTrue(Files.exists(project.resolve("build/DatasetA-out.xtf")));
            assertTrue(Files.readString(project.resolve("build/validation.log")).contains("...validate done"));
            try (var connection = DriverManager.getConnection(POSTGIS.getJdbcUrl(), POSTGIS.getUsername(), POSTGIS.getPassword());
                 var statement = connection.createStatement();
                 var rows = statement.executeQuery("select count(*) from ili2pgtest.boflaechen")) {
                assertTrue(rows.next());
                assertEquals(0, rows.getInt(1));
            }
        } finally {
            POSTGIS.stop();
        }
    }

    @AfterAll
    void closeNetwork() {
        NETWORK.close();
    }

    private void copyResource(String resource, String target) throws Exception {
        try (var input = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertTrue(input != null, "Missing test resource " + resource);
            Files.copy(input, project.resolve(target));
        }
    }
}
