package ch.so.agi.gretl;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorePluginFunctionalTest {

    @TempDir
    Path projectDir;

    @Test
    void appliesPluginAndCompressesFile() throws IOException {
        writeSettings();
        Files.writeString(projectDir.resolve("input.txt"), "GRETL core\n", StandardCharsets.UTF_8);
        writeBuild("""
                plugins { id 'ch.so.agi.gretl' }

                import ch.so.agi.gretl.tasks.Gzip

                tasks.register('compress', Gzip) {
                    dataFile 'input.txt'
                    gzipFile layout.buildDirectory.file('nested/out/input.txt.gz').get().asFile
                }
                """);

        run("compress");

        Path output = projectDir.resolve("build/nested/out/input.txt.gz");
        assertTrue(Files.exists(output));
        try (GZIPInputStream in = new GZIPInputStream(Files.newInputStream(output))) {
            assertEquals("GRETL core\n", new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    void transformsMultipleXmlFilesWithXslFile() throws IOException {
        writeSettings();
        Files.writeString(projectDir.resolve("one.xml"), "<root><name>One</name></root>", StandardCharsets.UTF_8);
        Files.writeString(projectDir.resolve("two.xml"), "<root><name>Two</name></root>", StandardCharsets.UTF_8);
        Files.writeString(projectDir.resolve("transform.xsl"), """
                <xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
                  <xsl:output method="text"/>
                  <xsl:template match="/">Hello <xsl:value-of select="/root/name"/></xsl:template>
                </xsl:stylesheet>
                """, StandardCharsets.UTF_8);
        writeBuild("""
                plugins { id 'ch.so.agi.gretl' }

                import ch.so.agi.gretl.tasks.XslTransformer

                tasks.register('transformXml', XslTransformer) {
                    xslFile 'transform.xsl'
                    xmlFiles 'one.xml', 'two.xml'
                    outDirectory layout.buildDirectory.dir('xsl').get().asFile
                    fileExtension 'txt'
                }
                """);

        run("transformXml");

        assertEquals("Hello One", Files.readString(projectDir.resolve("build/xsl/one.txt"), StandardCharsets.UTF_8));
        assertEquals("Hello Two", Files.readString(projectDir.resolve("build/xsl/two.txt"), StandardCharsets.UTF_8));
    }

    @Test
    void transformsXmlWithXslResource() throws IOException {
        writeSettings();
        Files.writeString(projectDir.resolve("input.xml"), "<root><name>GRETL</name></root>", StandardCharsets.UTF_8);
        writeBuild("""
                plugins { id 'ch.so.agi.gretl' }

                import ch.so.agi.gretl.tasks.XslTransformer

                tasks.register('transformXml', XslTransformer) {
                    xslResource 'name-to-text.xsl'
                    xmlFiles 'input.xml'
                    outDirectory layout.buildDirectory.dir('resource-xsl').get().asFile
                    fileExtension 'txt'
                }
                """);

        run("transformXml");

        assertEquals("Hello GRETL",
                Files.readString(projectDir.resolve("build/resource-xsl/input.txt"), StandardCharsets.UTF_8));
    }

    @Test
    void rejectsEmptyXmlCollection() throws IOException {
        writeSettings();
        Files.writeString(projectDir.resolve("transform.xsl"), """
                <xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"/>
                """, StandardCharsets.UTF_8);
        writeBuild("""
                plugins { id 'ch.so.agi.gretl' }

                import ch.so.agi.gretl.tasks.XslTransformer

                tasks.register('transformXml', XslTransformer) {
                    xslFile 'transform.xsl'
                    outDirectory layout.buildDirectory.dir('xsl').get().asFile
                }
                """);

        BuildResult result = runAndFail("transformXml");

        assertTrue(result.getOutput().contains("xmlFiles must not be empty"));
    }

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

        try (Connection connection = sqlite(projectDir.resolve("sql-executor.db"));
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("select name from colors where id = 1")) {
            assertTrue(rs.next());
            assertEquals("red", rs.getString(1));
        }
    }

    @Test
    void executesMultipleSqlFilesInOrder() throws Exception {
        writeSettings();
        Files.writeString(projectDir.resolve("schema.sql"), """
                CREATE TABLE colors (id INTEGER PRIMARY KEY, name TEXT);
                """, StandardCharsets.UTF_8);
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
        Files.writeString(projectDir.resolve("schema.sql"), """
                CREATE TABLE colors (id INTEGER PRIMARY KEY, name TEXT);
                """, StandardCharsets.UTF_8);
        Files.writeString(projectDir.resolve("insert.sql"), """
                INSERT INTO colors (id, name) VALUES (${id}, '${name}');
                """, StandardCharsets.UTF_8);
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
    void rollsBackEntireSqlExecutorTaskOnFailure() throws Exception {
        writeSettings();
        Path database = projectDir.resolve("rollback.db");
        try (Connection connection = sqlite(database); Statement statement = connection.createStatement()) {
            statement.execute("create table colors (id integer primary key, name text)");
        }
        Files.writeString(projectDir.resolve("insert.sql"), """
                INSERT INTO colors (id, name) VALUES (1, 'red');
                """, StandardCharsets.UTF_8);
        Files.writeString(projectDir.resolve("fail.sql"), """
                INSERT INTO missing_table (id) VALUES (1);
                """, StandardCharsets.UTF_8);
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
    void rollsBackAllSqlExecutorParameterSetsOnFailure() throws Exception {
        writeSettings();
        Path database = projectDir.resolve("parameter-rollback.db");
        try (Connection connection = sqlite(database); Statement statement = connection.createStatement()) {
            statement.execute("create table colors (id integer primary key, name text)");
        }
        Files.writeString(projectDir.resolve("insert.sql"), """
                INSERT INTO colors (id, name) VALUES (${id}, '${name}');
                """, StandardCharsets.UTF_8);
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
    void executesMultipleDb2DbTransfersInOrder() throws Exception {
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
    void db2DbParameterSetsRunAllTransfersInOneTargetTransaction() throws Exception {
        writeSettings();
        Path source = projectDir.resolve("source.db");
        Path target = projectDir.resolve("target.db");
        createColorTable(source);
        try (Connection connection = sqlite(source); Statement statement = connection.createStatement()) {
            statement.execute("alter table colors add column import_year integer");
            statement.execute("insert into colors (id, name, import_year) values (1, 'red', 2024)");
            statement.execute("insert into colors (id, name, import_year) values (2, 'blue', 2025)");
        }
        createColorTable(target);
        Files.writeString(projectDir.resolve("select-colors.sql"), """
                select id, name from colors where import_year = ${year};
                """, StandardCharsets.UTF_8);
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
    void db2DbRollsBackFirstTransferWhenSecondTransferFails() throws Exception {
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
    void db2DbRollsBackFirstParameterSetWhenSecondParameterSetFails() throws Exception {
        writeSettings();
        Path source = projectDir.resolve("source.db");
        Path target = projectDir.resolve("target.db");
        createColorTable(source);
        createColorTable(target);
        Files.writeString(projectDir.resolve("select-params.sql"), """
                select ${id} as id, '${name}' as name;
                """, StandardCharsets.UTF_8);
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
    void db2DbDeleteAllRowsParticipatesInTransaction() throws Exception {
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
    void db2DbFlushesConfiguredBatchSize() throws Exception {
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
    void db2DbDoesNotPrintDatabasePasswords() throws Exception {
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
    void supportsDb2DbKotlinDsl() throws Exception {
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
    void supportsSqlExecutorKotlinDsl() throws Exception {
        writeSettings();
        Files.writeString(projectDir.resolve("init.sql"), """
                CREATE TABLE colors (id INTEGER PRIMARY KEY, name TEXT);
                INSERT INTO colors (id, name) VALUES (1, 'red');
                """, StandardCharsets.UTF_8);
        writeKotlinBuild("""
                import ch.so.agi.gretl.tasks.SqlExecutor

                plugins { id("ch.so.agi.gretl") }

                tasks.register<SqlExecutor>("initDb") {
                    database("jdbc:sqlite:%s/kotlin.db")
                    sqlFiles("init.sql")
                }
                """.formatted(projectDir.toString().replace("\\", "\\\\")));

        run("initDb");

        assertEquals("red", scalar(projectDir.resolve("kotlin.db"), "select name from colors where id = 1"));
    }

    @Test
    void sqlExecutorDoesNotPrintDatabasePassword() throws Exception {
        writeSettings();
        Files.writeString(projectDir.resolve("init.sql"), """
                CREATE TABLE colors (id INTEGER PRIMARY KEY, name TEXT);
                """, StandardCharsets.UTF_8);
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

    private void createColorTable(Path database) throws Exception {
        try (Connection connection = sqlite(database); Statement statement = connection.createStatement()) {
            statement.execute("create table if not exists colors (id integer primary key, name text)");
        }
    }

    private void createFruitTable(Path database) throws Exception {
        try (Connection connection = sqlite(database); Statement statement = connection.createStatement()) {
            statement.execute("create table if not exists fruits (id integer primary key, name text)");
        }
    }

    private void insertColor(Path database, int id, String name) throws Exception {
        try (Connection connection = sqlite(database); Statement statement = connection.createStatement()) {
            statement.execute("insert into colors (id, name) values (" + id + ", '" + name + "')");
        }
    }

    private void insertFruit(Path database, int id, String name) throws Exception {
        try (Connection connection = sqlite(database); Statement statement = connection.createStatement()) {
            statement.execute("insert into fruits (id, name) values (" + id + ", '" + name + "')");
        }
    }

    private String scalar(Path database, String sql) throws Exception {
        try (Connection connection = sqlite(database);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            assertTrue(resultSet.next());
            return resultSet.getString(1);
        }
    }

    private int scalarInt(Path database, String sql) throws Exception {
        try (Connection connection = sqlite(database);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            assertTrue(resultSet.next());
            return resultSet.getInt(1);
        }
    }

    private BuildResult run(String... arguments) {
        return GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments(appendStacktrace(arguments))
                .forwardOutput()
                .build();
    }

    private BuildResult runAndFail(String... arguments) {
        return GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments(appendStacktrace(arguments))
                .forwardOutput()
                .buildAndFail();
    }

    private String[] appendStacktrace(String[] arguments) {
        String[] result = new String[arguments.length + 1];
        System.arraycopy(arguments, 0, result, 0, arguments.length);
        result[arguments.length] = "--stacktrace";
        return result;
    }

    private void writeSettings() throws IOException {
        Files.writeString(projectDir.resolve("settings.gradle"), "rootProject.name = 'core-test'\n", StandardCharsets.UTF_8);
    }

    private void writeBuild(String content) throws IOException {
        Files.writeString(projectDir.resolve("build.gradle"), content, StandardCharsets.UTF_8);
    }

    private void writeKotlinBuild(String content) throws IOException {
        Files.writeString(projectDir.resolve("build.gradle.kts"), content, StandardCharsets.UTF_8);
    }

    private Connection sqlite(Path path) throws Exception {
        Class.forName("org.sqlite.JDBC");
        return DriverManager.getConnection("jdbc:sqlite:" + path.toAbsolutePath());
    }
}
