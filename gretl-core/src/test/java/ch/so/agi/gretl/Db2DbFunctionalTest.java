package ch.so.agi.gretl;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class Db2DbFunctionalTest extends CoreFunctionalTestSupport {

    @Test
    void copiesRowsBetweenSqliteDatabases() throws Exception {
        writeSettings();
        Path source = projectDir.resolve("source.db");
        Path target = projectDir.resolve("target.db");
        createColorTable(source);
        insertColor(source, 1, "red");
        insertColor(source, 2, "blue");
        createColorTable(target);
        Files.writeString(projectDir.resolve("select-colors.sql"), "select id, name from colors;", StandardCharsets.UTF_8);
        writeBuild("""
                plugins { id 'ch.so.agi.gretl' }

                import ch.so.agi.gretl.tasks.Db2Db

                tasks.register('copyRows', Db2Db) {
                    sourceDatabase "jdbc:sqlite:${projectDir}/source.db"
                    targetDatabase "jdbc:sqlite:${projectDir}/target.db"
                    transfer "select-colors.sql", "colors", true
                }
                """);

        run("copyRows");

        assertEquals(2, scalarInt(target, "select count(*) from colors"));
    }

    @Test
    void executesMultipleTransfersInOrder() throws Exception {
        writeSettings();
        Path source = projectDir.resolve("source.db");
        Path target = projectDir.resolve("target.db");
        createColorTable(source);
        createFruitTable(source);
        insertColor(source, 1, "red");
        insertFruit(source, 1, "apple");
        createColorTable(target);
        createFruitTable(target);
        Files.writeString(projectDir.resolve("select-colors.sql"), "select id, name from colors;", StandardCharsets.UTF_8);
        Files.writeString(projectDir.resolve("select-fruits.sql"), "select id, name from fruits;", StandardCharsets.UTF_8);
        writeBuild("""
                plugins { id 'ch.so.agi.gretl' }

                import ch.so.agi.gretl.tasks.Db2Db

                tasks.register('copyRows', Db2Db) {
                    sourceDatabase "jdbc:sqlite:${projectDir}/source.db"
                    targetDatabase "jdbc:sqlite:${projectDir}/target.db"
                    transfer "select-colors.sql", "colors", true
                    transfer {
                        sqlFile "select-fruits.sql"
                        targetTable "fruits"
                        deleteAllRows true
                    }
                }
                """);

        run("copyRows");

        assertEquals(1, scalarInt(target, "select count(*) from colors"));
        assertEquals(1, scalarInt(target, "select count(*) from fruits"));
    }

    @Test
    void parameterSetsRunAllTransfersInOneTargetTransaction() throws Exception {
        writeSettings();
        Path source = projectDir.resolve("source.db");
        Path target = projectDir.resolve("target.db");
        createColorTable(source);
        try (var connection = sqlite(source); var statement = connection.createStatement()) {
            statement.execute("alter table colors add column import_year integer");
            statement.execute("insert into colors (id, name, import_year) values (1, 'red', 2024)");
            statement.execute("insert into colors (id, name, import_year) values (2, 'blue', 2025)");
        }
        createColorTable(target);
        Files.writeString(projectDir.resolve("select-colors.sql"), "select id, name from colors where import_year = ${year};", StandardCharsets.UTF_8);
        writeBuild("""
                plugins { id 'ch.so.agi.gretl' }

                import ch.so.agi.gretl.tasks.Db2Db

                tasks.register('copyRows', Db2Db) {
                    sourceDatabase "jdbc:sqlite:${projectDir}/source.db"
                    targetDatabase "jdbc:sqlite:${projectDir}/target.db"
                    transfer "select-colors.sql", "colors", false
                    sqlParameterSets([year: 2024], [year: 2025])
                }
                """);

        run("copyRows");

        assertEquals(2, scalarInt(target, "select count(*) from colors"));
    }

    @Test
    void rollsBackFirstTransferWhenSecondTransferFails() throws Exception {
        writeSettings();
        Path source = projectDir.resolve("source.db");
        Path target = projectDir.resolve("target.db");
        createColorTable(source);
        insertColor(source, 1, "red");
        createColorTable(target);
        Files.writeString(projectDir.resolve("select-colors.sql"), "select id, name from colors;", StandardCharsets.UTF_8);
        Files.writeString(projectDir.resolve("fail.sql"), "select id, name from missing_table;", StandardCharsets.UTF_8);
        writeBuild("""
                plugins { id 'ch.so.agi.gretl' }

                import ch.so.agi.gretl.tasks.Db2Db

                tasks.register('copyRows', Db2Db) {
                    sourceDatabase "jdbc:sqlite:${projectDir}/source.db"
                    targetDatabase "jdbc:sqlite:${projectDir}/target.db"
                    transfer "select-colors.sql", "colors", false
                    transfer "fail.sql", "colors", false
                }
                """);

        runAndFail("copyRows");

        assertEquals(0, scalarInt(target, "select count(*) from colors"));
    }

    @Test
    void rollsBackFirstParameterSetWhenSecondParameterSetFails() throws Exception {
        writeSettings();
        Path source = projectDir.resolve("source.db");
        Path target = projectDir.resolve("target.db");
        createColorTable(source);
        createColorTable(target);
        Files.writeString(projectDir.resolve("select-params.sql"), "select ${id} as id, '${name}' as name;", StandardCharsets.UTF_8);
        writeBuild("""
                plugins { id 'ch.so.agi.gretl' }

                import ch.so.agi.gretl.tasks.Db2Db

                tasks.register('copyRows', Db2Db) {
                    sourceDatabase "jdbc:sqlite:${projectDir}/source.db"
                    targetDatabase "jdbc:sqlite:${projectDir}/target.db"
                    transfer "select-params.sql", "colors", false
                    sqlParameterSets([id: 1, name: 'red'], [id: 1, name: 'duplicate'])
                }
                """);

        runAndFail("copyRows");

        assertEquals(0, scalarInt(target, "select count(*) from colors"));
    }

    @Test
    void deleteAllRowsParticipatesInTransaction() throws Exception {
        writeSettings();
        Path source = projectDir.resolve("source.db");
        Path target = projectDir.resolve("target.db");
        createColorTable(source);
        insertColor(source, 2, "blue");
        createColorTable(target);
        insertColor(target, 1, "red");
        Files.writeString(projectDir.resolve("select-colors.sql"), "select id, name from colors;", StandardCharsets.UTF_8);
        Files.writeString(projectDir.resolve("fail.sql"), "select id, name from missing_table;", StandardCharsets.UTF_8);
        writeBuild("""
                plugins { id 'ch.so.agi.gretl' }

                import ch.so.agi.gretl.tasks.Db2Db

                tasks.register('copyRows', Db2Db) {
                    sourceDatabase "jdbc:sqlite:${projectDir}/source.db"
                    targetDatabase "jdbc:sqlite:${projectDir}/target.db"
                    transfer "select-colors.sql", "colors", true
                    transfer "fail.sql", "colors", false
                }
                """);

        runAndFail("copyRows");

        assertEquals("red", scalar(target, "select name from colors where id = 1"));
        assertEquals(1, scalarInt(target, "select count(*) from colors"));
    }

    @Test
    void flushesConfiguredBatchSize() throws Exception {
        writeSettings();
        Path source = projectDir.resolve("source.db");
        Path target = projectDir.resolve("target.db");
        createColorTable(source);
        for (int i = 1; i <= 5; i++) {
            insertColor(source, i, "color-" + i);
        }
        createColorTable(target);
        Files.writeString(projectDir.resolve("select-colors.sql"), "select id, name from colors order by id;", StandardCharsets.UTF_8);
        writeBuild("""
                plugins { id 'ch.so.agi.gretl' }

                import ch.so.agi.gretl.tasks.Db2Db

                tasks.register('copyRows', Db2Db) {
                    sourceDatabase "jdbc:sqlite:${projectDir}/source.db"
                    targetDatabase "jdbc:sqlite:${projectDir}/target.db"
                    transfer "select-colors.sql", "colors", false
                    batchSize 2
                }
                """);

        run("copyRows");

        assertEquals(5, scalarInt(target, "select count(*) from colors"));
    }

    @Test
    void supportsQuotedTargetIdentifiers() throws Exception {
        writeSettings();
        Path source = projectDir.resolve("source.db");
        Path target = projectDir.resolve("target.db");
        createColorTable(source);
        insertColor(source, 1, "red");
        try (var connection = sqlite(target); var statement = connection.createStatement()) {
            statement.execute("create table \"target colors\" (id integer primary key, name text)");
        }
        Files.writeString(projectDir.resolve("select-colors.sql"), "select id, name from colors;", StandardCharsets.UTF_8);
        writeBuild("""
                plugins { id 'ch.so.agi.gretl' }

                import ch.so.agi.gretl.tasks.Db2Db

                tasks.register('copyRows', Db2Db) {
                    sourceDatabase "jdbc:sqlite:${projectDir}/source.db"
                    targetDatabase "jdbc:sqlite:${projectDir}/target.db"
                    transfer 'select-colors.sql', '"target colors"', false
                }
                """);

        run("copyRows");

        assertEquals(1, scalarInt(target, "select count(*) from \"target colors\""));
    }

    @Test
    void doesNotPrintDatabasePasswords() throws Exception {
        writeSettings();
        Path source = projectDir.resolve("source.db");
        Path target = projectDir.resolve("target.db");
        createColorTable(source);
        createColorTable(target);
        Files.writeString(projectDir.resolve("select-colors.sql"), "select id, name from colors;", StandardCharsets.UTF_8);
        writeBuild("""
                plugins { id 'ch.so.agi.gretl' }

                import ch.so.agi.gretl.tasks.Db2Db

                def sourcePass = "source" + "Secret"
                def targetPass = "target" + "Secret"

                tasks.register('copyRows', Db2Db) {
                    sourceDatabase "jdbc:sqlite:${projectDir}/source.db", "user", sourcePass
                    targetDatabase "jdbc:sqlite:${projectDir}/target.db", "user", targetPass
                    transfer "select-colors.sql", "colors", false
                }
                """);

        BuildResult result = run("copyRows", "--debug");

        assertFalse(result.getOutput().contains("sourceSecret"));
        assertFalse(result.getOutput().contains("targetSecret"));
    }

    @Test
    void supportsKotlinDsl() throws Exception {
        writeSettings();
        Path source = projectDir.resolve("source.db");
        Path target = projectDir.resolve("target.db");
        createColorTable(source);
        insertColor(source, 1, "red");
        createColorTable(target);
        Files.writeString(projectDir.resolve("select-colors.sql"), "select id, name from colors;", StandardCharsets.UTF_8);
        writeKotlinBuild("""
                import ch.so.agi.gretl.tasks.Db2Db

                plugins { id("ch.so.agi.gretl") }

                tasks.register<Db2Db>("copyRows") {
                    sourceDatabase("jdbc:sqlite:%s/source.db")
                    targetDatabase("jdbc:sqlite:%s/target.db")
                    transfer("select-colors.sql", "colors", true)
                }
                """.formatted(projectDir.toString().replace("\\", "\\\\"),
                projectDir.toString().replace("\\", "\\\\")));

        run("copyRows");

        assertEquals(1, scalarInt(target, "select count(*) from colors"));
    }

    @Test
    void supportsKotlinDslTransferBlockWithGeometryColumns() throws Exception {
        writeSettings();
        writeKotlinBuild("""
                import ch.so.agi.gretl.tasks.Db2Db

                plugins { id("ch.so.agi.gretl") }

                tasks.register<Db2Db>("copyRows") {
                    sourceDatabase("jdbc:postgresql://localhost/source", "reader", "secret")
                    targetDatabase("jdbc:postgresql://localhost/target", "writer", "secret")
                    transfer {
                        sqlFile("sql/select-parcels.sql")
                        targetTable("public.parcels")
                        deleteAllRows(true)
                        geometryColumns("geom:WKT:2056", "shape:WKB:2056")
                    }
                    batchSize(1000)
                    fetchSize(1000)
                }
                """);

        run("tasks", "--all");
    }
}
