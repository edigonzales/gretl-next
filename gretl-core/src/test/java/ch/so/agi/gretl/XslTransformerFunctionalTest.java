package ch.so.agi.gretl;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XslTransformerFunctionalTest extends CoreFunctionalTestSupport {

    @Test
    void transformsMultipleXmlFilesWithXslFile() throws Exception {
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
    void transformsOriginalGretlXmlWithFileXslFixture() throws Exception {
        writeSettings();
        copyResourceTree("original-gretl/xsl", projectDir);
        writeBuild("""
                plugins { id 'ch.so.agi.gretl' }

                import ch.so.agi.gretl.tasks.XslTransformer

                tasks.register('transformXml', XslTransformer) {
                    xslFile 'eCH0132_to_SO_AGI_SGV_Meldungen_20221109.xsl'
                    xmlFiles 'MeldungAnGeometer_G-0098981_20230214_104054_Koordinaten.xml'
                    outDirectory layout.buildDirectory.dir('xsl').get().asFile
                }
                """);

        run("transformXml");

        String fileContent = Files.readString(
                projectDir.resolve("build/xsl/MeldungAnGeometer_G-0098981_20230214_104054_Koordinaten.xtf"),
                StandardCharsets.UTF_8);
        assertOriginalSgvContent(fileContent);
    }

    @Test
    void supportsKotlinDslWithXslFile() throws Exception {
        writeSettings();
        Files.writeString(projectDir.resolve("input.xml"), "<root><name>Kotlin</name></root>", StandardCharsets.UTF_8);
        Files.writeString(projectDir.resolve("transform.xsl"), """
                <xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
                  <xsl:output method="text"/>
                  <xsl:template match="/">Hello <xsl:value-of select="/root/name"/></xsl:template>
                </xsl:stylesheet>
                """, StandardCharsets.UTF_8);
        writeKotlinBuild("""
                import ch.so.agi.gretl.tasks.XslTransformer

                plugins { id("ch.so.agi.gretl") }

                tasks.register<XslTransformer>("transformXml") {
                    xslFile("transform.xsl")
                    xmlFiles("input.xml")
                    outDirectory(layout.buildDirectory.dir("xsl").get().asFile)
                    fileExtension("txt")
                }
                """);

        run("transformXml");

        assertEquals("Hello Kotlin", Files.readString(projectDir.resolve("build/xsl/input.txt"), StandardCharsets.UTF_8));
    }

    @Test
    void transformsOriginalGretlXmlWithResourceXslFixture() throws Exception {
        writeSettings();
        copyResource("original-gretl/xsl/MeldungAnGeometer_G-0098981_20230214_104054_Koordinaten.xml",
                "input.xml");
        writeBuild("""
                plugins { id 'ch.so.agi.gretl' }

                import ch.so.agi.gretl.tasks.XslTransformer

                tasks.register('transformXml', XslTransformer) {
                    xslResource 'eCH0132_to_SO_AGI_SGV_Meldungen_20221109.xsl'
                    xmlFiles 'input.xml'
                    outDirectory layout.buildDirectory.dir('resource-xsl').get().asFile
                }
                """);

        run("transformXml");

        String fileContent = Files.readString(projectDir.resolve("build/resource-xsl/input.xtf"), StandardCharsets.UTF_8);
        assertOriginalSgvContent(fileContent);
    }

    @Test
    void transformsOriginalGretlXmlFileSetWithCustomExtension() throws Exception {
        writeSettings();
        copyResourceTree("original-gretl/xsl", projectDir);
        writeBuild("""
                plugins { id 'ch.so.agi.gretl' }

                import ch.so.agi.gretl.tasks.XslTransformer

                tasks.register('transformXml', XslTransformer) {
                    xslFile 'eCH0132_to_SO_AGI_SGV_Meldungen_20221109.xsl'
                    xmlFiles fileTree('.').matching { include '*.xml' }
                    outDirectory layout.buildDirectory.dir('xsl-set').get().asFile
                    fileExtension 'out'
                }
                """);

        run("transformXml");

        assertTrue(Files.exists(projectDir.resolve("build/xsl-set/MeldungAnGeometer_G-0098981_20230214_104054_Koordinaten.out")));
        assertTrue(Files.exists(projectDir.resolve("build/xsl-set/MeldungAnGeometer_mehrere_gebaeude_mehrere_grundstuecke.out")));
    }

    @Test
    void rejectsEmptyXmlCollection() throws Exception {
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

    private void assertOriginalSgvContent(String fileContent) {
        assertTrue(fileContent.contains("<SO_AGI_SGV_Meldungen_20221109.Meldungen BID=\"SO_AGI_SGV_Meldungen_20221109.Meldungen\">"));
        assertTrue(fileContent.contains("<Grundstuecksnummer>1505</Grundstuecksnummer>"));
        assertTrue(fileContent.contains("<Gebaeudebezeichnung>Reine Wohngebäude (Wohnnutzung ausschliesslich)</Gebaeudebezeichnung>"));
    }
}
