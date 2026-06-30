package ch.so.agi.gretl;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlExecutorFunctionalTest extends CoreFunctionalTestSupport {

    @Test
    void executesSqlAgainstSqlite() throws Exception {
        writeSettings();
        Files.writeString(projectDir.resolve("init.sql"), """
                CREATE TABLE colors (id INTEGER PRIMARY KEY, name TEXT);
                INSERT INTO colors (id, name) VALUES (1, 'red');
                """, StandardCharsets.UTF_8);
        writeBuild("""
                plugins { id 'ch.so.agi.gretl' }

                import ch.so.agi.gretl.tasks.SqlExecutor

                tasks.register('initDb', SqlExecutor) {
                    database "jdbc:sqlite:${projectDir}/sql-executor.db"
                    sqlFiles "init.sql"
                }
                """);

        run("initDb");

        assertEquals("red", scalar(projectDir.resolve("sql-executor.db"), "select name from colors where id = 1"));
    }

    @Test
    void executesMultipleSqlFilesInOrder() throws Exception {
        writeSettings();
        Files.writeString(projectDir.resolve("schema.sql"), "CREATE TABLE colors (id INTEGER PRIMARY KEY, name TEXT);", StandardCharsets.UTF_8);
        Files.writeString(projectDir.resolve("data.sql"), """
                INSERT INTO colors (id, name) VALUES (1, 'red');
                INSERT INTO colors (id, name) VALUES (2, 'blue');
                """, StandardCharsets.UTF_8);
        writeBuild("""
                plugins { id 'ch.so.agi.gretl' }

                import ch.so.agi.gretl.tasks.SqlExecutor

                tasks.register('initDb', SqlExecutor) {
                    database "jdbc:sqlite:${projectDir}/ordered.db"
                    sqlFiles "schema.sql", "data.sql"
                }
                """);

        run("initDb");

        assertEquals("red,blue", scalar(projectDir.resolve("ordered.db"),
                "select group_concat(name, ',') from (select name from colors order by id)"));
    }

    @Test
    void executesParameterSetsInOneTaskTransaction() throws Exception {
        writeSettings();
        Files.writeString(projectDir.resolve("schema.sql"), "CREATE TABLE colors (id INTEGER PRIMARY KEY, name TEXT);", StandardCharsets.UTF_8);
        Files.writeString(projectDir.resolve("insert.sql"), "INSERT INTO colors (id, name) VALUES (${id}, '${name}');", StandardCharsets.UTF_8);
        writeBuild("""
                plugins { id 'ch.so.agi.gretl' }

                import ch.so.agi.gretl.tasks.SqlExecutor

                tasks.register('initDb', SqlExecutor) {
                    database "jdbc:sqlite:${projectDir}/parameters.db"
                    sqlFiles "schema.sql"
                }

                tasks.register('insertRows', SqlExecutor) {
                    dependsOn 'initDb'
                    database "jdbc:sqlite:${projectDir}/parameters.db"
                    sqlFiles "insert.sql"
                    sqlParameterSets([id: 1, name: 'red'], [id: 2, name: 'blue'])
                }
                """);

        run("insertRows");

        assertEquals(2, scalarInt(projectDir.resolve("parameters.db"), "select count(*) from colors"));
    }

    @Test
    void rollsBackEntireTaskOnFailure() throws Exception {
        writeSettings();
        Path database = projectDir.resolve("rollback.db");
        try (var connection = sqlite(database); var statement = connection.createStatement()) {
            statement.execute("create table colors (id integer primary key, name text)");
        }
        Files.writeString(projectDir.resolve("insert.sql"), "INSERT INTO colors (id, name) VALUES (1, 'red');", StandardCharsets.UTF_8);
        Files.writeString(projectDir.resolve("fail.sql"), "INSERT INTO missing_table (id) VALUES (1);", StandardCharsets.UTF_8);
        writeBuild("""
                plugins { id 'ch.so.agi.gretl' }

                import ch.so.agi.gretl.tasks.SqlExecutor

                tasks.register('writeThenFail', SqlExecutor) {
                    database "jdbc:sqlite:${projectDir}/rollback.db"
                    sqlFiles "insert.sql", "fail.sql"
                }
                """);

        runAndFail("writeThenFail");

        assertEquals(0, scalarInt(database, "select count(*) from colors"));
    }

    @Test
    void rollsBackAllParameterSetsOnFailure() throws Exception {
        writeSettings();
        Path database = projectDir.resolve("parameter-rollback.db");
        try (var connection = sqlite(database); var statement = connection.createStatement()) {
            statement.execute("create table colors (id integer primary key, name text)");
        }
        Files.writeString(projectDir.resolve("insert.sql"), "INSERT INTO colors (id, name) VALUES (${id}, '${name}');", StandardCharsets.UTF_8);
        writeBuild("""
                plugins { id 'ch.so.agi.gretl' }

                import ch.so.agi.gretl.tasks.SqlExecutor

                tasks.register('insertRows', SqlExecutor) {
                    database "jdbc:sqlite:${projectDir}/parameter-rollback.db"
                    sqlFiles "insert.sql"
                    sqlParameterSets([id: 1, name: 'red'], [id: 1, name: 'duplicate'])
                }
                """);

        runAndFail("insertRows");

        assertEquals(0, scalarInt(database, "select count(*) from colors"));
    }

    @Test
    void executesDuckDbFixture() throws Exception {
        writeSettings();
        copyResourceTree("fixtures/duckdb", projectDir);
        writeBuild("""
                plugins { id 'ch.so.agi.gretl' }

                import ch.so.agi.gretl.tasks.SqlExecutor

                def dbUri = "jdbc:duckdb:${projectDir}/foo.duckdb"

                tasks.register('initDuckDb', SqlExecutor) {
                    database dbUri
                    sqlFiles 'init.sql'
                }

                tasks.register('createTable', SqlExecutor) {
                    dependsOn 'initDuckDb'
                    database dbUri
                    sqlFiles 'create_insert.sql'
                }

                tasks.register('importParquet', SqlExecutor) {
                    dependsOn 'createTable'
                    database dbUri
                    sqlFiles 'import_parquet.sql'
                    sqlParameters([pwd: "'${projectDir}'"])
                }
                """);

        run("importParquet");

        assertTrue(Files.exists(projectDir.resolve("foo.duckdb")));
    }

    @Test
    void supportsKotlinDslWithParameterSets() throws Exception {
        writeSettings();
        Files.writeString(projectDir.resolve("schema.sql"),
                "CREATE TABLE colors (id INTEGER PRIMARY KEY, name TEXT);", StandardCharsets.UTF_8);
        Files.writeString(projectDir.resolve("insert.sql"),
                "INSERT INTO colors (id, name) VALUES (${id}, ${name});", StandardCharsets.UTF_8);
        writeKotlinBuild("""
                import ch.so.agi.gretl.tasks.SqlExecutor

                plugins { id("ch.so.agi.gretl") }

                tasks.register<SqlExecutor>("initDb") {
                    database("jdbc:sqlite:%s/kotlin.db")
                    sqlFiles("schema.sql")
                }

                tasks.register<SqlExecutor>("insertRows") {
                    dependsOn("initDb")
                    database("jdbc:sqlite:%s/kotlin.db")
                    sqlFiles("insert.sql")
                    sqlParameterSets(
                        mapOf("id" to 1, "name" to "'red'"),
                        mapOf("id" to 2, "name" to "'blue'")
                    )
                }
                """.formatted(
                projectDir.toString().replace("\\", "\\\\"),
                projectDir.toString().replace("\\", "\\\\")));

        run("insertRows");

        assertEquals("red,blue", scalar(projectDir.resolve("kotlin.db"),
                "select group_concat(name, ',') from (select name from colors order by id)"));
    }

    @Test
    void doesNotPrintDatabasePassword() throws Exception {
        writeSettings();
        Files.writeString(projectDir.resolve("init.sql"), "CREATE TABLE colors (id INTEGER PRIMARY KEY, name TEXT);", StandardCharsets.UTF_8);
        writeBuild("""
                plugins { id 'ch.so.agi.gretl' }

                import ch.so.agi.gretl.tasks.SqlExecutor

                def dbPass = "super" + "Secret"

                tasks.register('initDb', SqlExecutor) {
                    database "jdbc:sqlite:${projectDir}/secret.db", "user", dbPass
                    sqlFiles "init.sql"
                }
                """);

        BuildResult result = run("initDb", "--debug");

        assertFalse(result.getOutput().contains("superSecret"));
    }
}
