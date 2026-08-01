package ch.so.agi.gretl.combined;

import ch.so.agi.gretl.test.tags.GretlTestTags;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag(GretlTestTags.PUBLISHED_ARTIFACT_ONLY)
class CombinedPublishedArtifactContractFunctionalTest extends CombinedPluginTestSupport {

    @Test
    void publishedCoreAndGeoToolsArtifactsUseTheSameVersion() throws Exception {
        assertEquals(pluginVersion(), pom("ch.so.agi", "gretl-core").getElementsByTagName("version").item(0).getTextContent());
        assertEquals(pluginVersion(), pom("ch.so.agi", "gretl-geotools").getElementsByTagName("version").item(0).getTextContent());
    }

    @Test
    void pluginMarkersPointToThePublishedImplementationArtifacts() throws Exception {
        assertMarker("ch.so.agi.gretl", "ch.so.agi.gretl.gradle.plugin", "gretl-core");
        assertMarker("ch.so.agi.gretl.geotools", "ch.so.agi.gretl.geotools.gradle.plugin", "gretl-geotools");
    }

    @Test
    void publishedJarsContainTheirPluginDescriptors() throws Exception {
        assertDescriptor("gretl-core", "ch.so.agi.gretl", "ch.so.agi.gretl.gradle.GretlPlugin");
        assertDescriptor("gretl-geotools", "ch.so.agi.gretl.geotools", "ch.so.agi.gretl.geotools.GretlGeotoolsPlugin");
    }

    @Test
    void publishedGeoToolsJarContainsTheEmbeddedWorkerRuntime() throws Exception {
        try (JarFile jar = new JarFile(artifact("ch.so.agi", "gretl-geotools", "jar").toFile())) {
            List<String> entries = jar.stream().map(java.util.jar.JarEntry::getName).toList();
            assertTrue(entries.stream().anyMatch(entry -> entry.matches(
                    "gretl-geotools-worker-classpath/gretl-geotools-[^/]+-worker-runtime.jar")));
            for (String library : List.of("gt-main-", "gt-geotiff-", "gt-coverage-", "gt-shapefile-", "gt-epsg-hsql-")) {
                assertTrue(entries.stream().anyMatch(entry -> entry.contains("/lib/" + library)), library);
            }
        }
    }

    @Test
    void publishedConsumerCanResolveBothPluginIdsTogether() throws Exception {
        writeSettings();
        writeGroovyBuild("""
                plugins {
                    id 'ch.so.agi.gretl'
                    id 'ch.so.agi.gretl.geotools'
                }
                tasks.register('publishedContract') {
                    doLast {
                        assert pluginManager.hasPlugin('ch.so.agi.gretl')
                        assert pluginManager.hasPlugin('ch.so.agi.gretl.geotools')
                        assert tasks.findByName('readShapefile') != null
                        println 'PUBLISHED_COMBINED_RESOLUTION=OK'
                    }
                }
                """);
        BuildResult result = run("publishedContract");
        assertTrue(result.getOutput().contains("PUBLISHED_COMBINED_RESOLUTION=OK"));
    }

    @Test
    void nonexistentPublishedPluginVersionFailsResolution() throws Exception {
        String repository = Path.of(System.getProperty("gretl.test.publishedRepository"))
                .toAbsolutePath().normalize().toUri().toString();
        Files.writeString(projectPath("settings.gradle"), """
                pluginManagement {
                    repositories { maven { url = uri('%s') } }
                    plugins {
                        id 'ch.so.agi.gretl' version '9.9.9'
                        id 'ch.so.agi.gretl.geotools' version '9.9.9'
                    }
                }
                rootProject.name = 'missing-published-version'
                """.formatted(repository));
        writeGroovyBuild("plugins { id 'ch.so.agi.gretl'; id 'ch.so.agi.gretl.geotools' }\n");

        BuildResult result = runAndFail("tasks");
        assertTrue(result.getOutput().contains("9.9.9"), result.getOutput());
    }

    private String pluginVersion() {
        return System.getProperty("gretl.test.pluginVersion");
    }

    private Document pom(String group, String artifact) throws Exception {
        return parse(artifact(group, artifact, "pom"));
    }

    private void assertMarker(String markerGroup, String markerArtifact, String implementationArtifact) throws Exception {
        Document marker = parse(artifact(markerGroup, markerArtifact, "pom"));
        var dependencies = marker.getElementsByTagName("dependency");
        assertEquals(1, dependencies.getLength());
        var dependency = (org.w3c.dom.Element) dependencies.item(0);
        assertEquals("ch.so.agi", dependency.getElementsByTagName("groupId").item(0).getTextContent());
        assertEquals(implementationArtifact, dependency.getElementsByTagName("artifactId").item(0).getTextContent());
        assertEquals(pluginVersion(), dependency.getElementsByTagName("version").item(0).getTextContent());
    }

    private void assertDescriptor(String artifact, String pluginId, String implementationClass) throws Exception {
        try (JarFile jar = new JarFile(artifact("ch.so.agi", artifact, "jar").toFile())) {
            var entry = jar.getJarEntry("META-INF/gradle-plugins/" + pluginId + ".properties");
            assertTrue(entry != null, "Missing descriptor for " + pluginId);
            var properties = new java.util.Properties();
            try (var input = jar.getInputStream(entry)) {
                properties.load(input);
            }
            assertEquals(implementationClass, properties.getProperty("implementation-class"));
        }
    }

    private Path artifact(String group, String artifact, String extension) throws IOException {
        Path versionDirectory = Path.of(System.getProperty("gretl.test.publishedRepository"))
                .resolve(group.replace('.', '/')).resolve(artifact).resolve(pluginVersion());
        if (!Files.isDirectory(versionDirectory)) {
            throw new IOException("Missing published artifact directory: " + versionDirectory);
        }
        String logicalName = artifact + "-" + pluginVersion() + "." + extension;
        Path logical = versionDirectory.resolve(logicalName);
        if (Files.isRegularFile(logical)) {
            return logical;
        }
        try (var paths = Files.list(versionDirectory)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith(artifact + "-"))
                    .filter(path -> path.getFileName().toString().endsWith("." + extension))
                    .filter(path -> !path.getFileName().toString().contains("-sources."))
                    .filter(path -> !path.getFileName().toString().contains("-javadoc."))
                    .max((left, right) -> left.getFileName().toString().compareTo(right.getFileName().toString()))
                    .orElseThrow(() -> new IOException("Missing published artifact: " + artifact));
        }
    }

    private Document parse(Path file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return factory.newDocumentBuilder().parse(file.toFile());
    }
}
