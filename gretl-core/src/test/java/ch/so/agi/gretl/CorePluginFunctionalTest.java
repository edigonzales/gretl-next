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
                    dataFile.set(file('input.txt'))
                    gzipFile.set(layout.buildDirectory.file('out/input.txt.gz').get().asFile)
                }
                """);

        run("compress");

        Path output = projectDir.resolve("build/out/input.txt.gz");
        assertTrue(Files.exists(output));
        try (GZIPInputStream in = new GZIPInputStream(Files.newInputStream(output))) {
            assertEquals("GRETL core\n", new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    void transformsXmlWithXsl() throws IOException {
        writeSettings();
        Files.writeString(projectDir.resolve("input.xml"), "<root><name>GRETL</name></root>", StandardCharsets.UTF_8);
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
                    xslFile = file('transform.xsl')
                    xmlFile = files('input.xml')
                    outDirectory = layout.buildDirectory.dir('xsl').get().asFile
                    fileExtension = 'txt'
                }
                """);

        run("transformXml");

        assertEquals("Hello GRETL", Files.readString(projectDir.resolve("build/xsl/input.txt"), StandardCharsets.UTF_8));
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
                    database.set(["jdbc:sqlite:${projectDir}/sql-executor.db"])
                    sqlFiles.set(files('init.sql'))
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
    void copiesRowsBetweenSqliteDatabases() throws Exception {
        writeSettings();
        Path source = projectDir.resolve("source.db");
        Path target = projectDir.resolve("target.db");
        try (Connection connection = sqlite(source); Statement statement = connection.createStatement()) {
            statement.execute("create table colors (id integer primary key, name text)");
            statement.execute("insert into colors (id, name) values (1, 'red'), (2, 'blue')");
        }
        try (Connection connection = sqlite(target); Statement statement = connection.createStatement()) {
            statement.execute("create table colors (id integer primary key, name text)");
        }
        Files.writeString(projectDir.resolve("select-colors.sql"), "select id, name from colors;", StandardCharsets.UTF_8);
        writeBuild("""
                plugins { id 'ch.so.agi.gretl' }

                import ch.so.agi.gretl.api.TransferSet
                import ch.so.agi.gretl.tasks.Db2Db

                tasks.register('copyRows', Db2Db) {
                    sourceDb.set(["jdbc:sqlite:${projectDir}/source.db"])
                    targetDb.set(["jdbc:sqlite:${projectDir}/target.db"])
                    transferSets.set([new TransferSet('select-colors.sql', 'colors', true)])
                }
                """);

        run("copyRows");

        try (Connection connection = sqlite(target);
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("select count(*) from colors")) {
            assertTrue(rs.next());
            assertEquals(2, rs.getInt(1));
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

    private Connection sqlite(Path path) throws Exception {
        Class.forName("org.sqlite.JDBC");
        return DriverManager.getConnection("jdbc:sqlite:" + path.toAbsolutePath());
    }
}
