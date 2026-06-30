package ch.so.agi.gretl;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlExecutorPostgisIntegrationTest extends PostgisIntegrationTestSupport {

    @Test
    void executesOriginalTaskChainFixture() throws Exception {
        writeSettings();
        copyResourceTree("fixtures/jobs/sqlexecutor", projectDir.resolve("sql"));
        createOrReplaceSchema("sqlexecutertaskchain");
        createSqlExecutorAlbumTables("sqlexecutertaskchain");
        writeBuild(sqlExecutorBuild("""
                tasks.register('runSql', SqlExecutor) {
                    database pgUrl, pgUser, pgPass
                    sqlFiles 'sql/statementChain_fillSrcTable.sql', 'sql/statementChain_insertIntoDestTable.sql'
                    sqlParameters([srcTable: 'albums_src'])
                }
                """));

        run("runSql");

        assertEquals(4, count("select count(*) from sqlexecutertaskchain.albums_dest"));
    }

    @Test
    void executesOriginalRelativePathFixture() throws Exception {
        writeSettings();
        copyResourceTree("fixtures/jobs/sqlexecutor", projectDir.resolve("sql"));
        createOrReplaceSchema("sqlexecuterrelpath");
        createSqlExecutorAlbumTables("sqlexecuterrelpath");
        insertAlbumRows("sqlexecuterrelpath", "src", 1);
        writeBuild(sqlExecutorBuild("""
                tasks.register('runSql', SqlExecutor) {
                    database pgUrl, pgUser, pgPass
                    sqlFiles 'sql/relativePathConfiguration.sql'
                }
                """));

        run("runSql");

        assertEquals(1, count("select count(*) from sqlexecuterrelpath.albums_dest"));
    }

    @Test
    void executesOriginalSingleParameterFixture() throws Exception {
        writeSettings();
        copyResourceTree("fixtures/jobs/sqlexecutor", projectDir.resolve("sql"));
        createSqlexecTable();
        writeBuild(sqlExecutorBuild("""
                tasks.register('runSql', SqlExecutor) {
                    database pgUrl, pgUser, pgPass
                    sqlFiles 'sql/parameter.sql'
                    sqlParameters([title: "'ele1'"])
                }
                """));

        run("runSql");

        assertEquals(Set.of("ele1"), stringSet("select title from sqlexec.src"));
    }

    @Test
    void executesOriginalParameterListFixture() throws Exception {
        writeSettings();
        copyResourceTree("fixtures/jobs/sqlexecutor", projectDir.resolve("sql"));
        createSqlexecTable();
        writeBuild(sqlExecutorBuild("""
                tasks.register('runSql', SqlExecutor) {
                    database pgUrl, pgUser, pgPass
                    sqlFiles 'sql/parameter-list.sql'
                    sqlParameterSets([title: "'ele1'"], [title: "'ele2'"])
                }
                """));

        run("runSql");

        assertEquals(Set.of("ele1", "ele2"), stringSet("select title from sqlexec.src"));
    }

    @Test
    void executesPostgisStatement() throws Exception {
        writeSettings();
        Files.writeString(projectDir.resolve("postgis.sql"), "select postgis_full_version();", StandardCharsets.UTF_8);
        writeBuild(sqlExecutorBuild("""
                tasks.register('runSql', SqlExecutor) {
                    database pgUrl, pgUser, pgPass
                    sqlFiles 'postgis.sql'
                }
                """));

        BuildResult result = run("runSql");

        assertTrue(result.getOutput().contains("POSTGIS"));
    }

    @Test
    void rollsBackCompleteTaskOnFailure() throws Exception {
        writeSettings();
        createOrReplaceSchema("sqlexecutorrollback");
        try (var connection = pg(); var statement = connection.createStatement()) {
            statement.execute("create table sqlexecutorrollback.colors (id integer primary key, name text)");
        }
        Files.writeString(projectDir.resolve("insert.sql"),
                "insert into sqlexecutorrollback.colors values (1, 'red');", StandardCharsets.UTF_8);
        Files.writeString(projectDir.resolve("fail.sql"),
                "insert into sqlexecutorrollback.missing values (1);", StandardCharsets.UTF_8);
        writeBuild(sqlExecutorBuild("""
                tasks.register('runSql', SqlExecutor) {
                    database pgUrl, pgUser, pgPass
                    sqlFiles 'insert.sql', 'fail.sql'
                }
                """));

        runAndFail("runSql");

        assertEquals(0, count("select count(*) from sqlexecutorrollback.colors"));
    }

    @Test
    void rollsBackAllParameterSetsOnFailure() throws Exception {
        writeSettings();
        createOrReplaceSchema("sqlexecutorparamrollback");
        try (var connection = pg(); var statement = connection.createStatement()) {
            statement.execute("create table sqlexecutorparamrollback.colors (id integer primary key, name text)");
        }
        Files.writeString(projectDir.resolve("insert.sql"),
                "insert into sqlexecutorparamrollback.colors values (${id}, '${name}');", StandardCharsets.UTF_8);
        writeBuild(sqlExecutorBuild("""
                tasks.register('runSql', SqlExecutor) {
                    database pgUrl, pgUser, pgPass
                    sqlFiles 'insert.sql'
                    sqlParameterSets([id: 1, name: 'red'], [id: 1, name: 'duplicate'])
                }
                """));

        runAndFail("runSql");

        assertEquals(0, count("select count(*) from sqlexecutorparamrollback.colors"));
    }

    private void createSqlexecTable() throws Exception {
        createOrReplaceSchema("sqlexec");
        try (var connection = pg(); var statement = connection.createStatement()) {
            statement.execute("create table sqlexec.src(title text)");
        }
    }

    private String sqlExecutorBuild(String taskDefinition) {
        return """
                plugins { id 'ch.so.agi.gretl' }

                import ch.so.agi.gretl.tasks.SqlExecutor

                def pgUrl = providers.gradleProperty('pgUrl').get()
                def pgUser = providers.gradleProperty('pgUser').get()
                def pgPass = providers.gradleProperty('pgPass').get()

                %s
                """.formatted(taskDefinition);
    }
}
