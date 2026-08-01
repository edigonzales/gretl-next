package ch.so.agi.gretl.combined;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CombinedBuildServiceFunctionalTest extends CombinedPluginTestSupport {

    @Test
    void coreAndGeoToolsUseDistinctSharedServiceNames() throws Exception {
        inspectServices();
    }

    @Test
    void coreTasksAreBoundToCoreService() throws Exception {
        inspectServices();
    }

    @Test
    void geoToolsTasksAreBoundToGeoToolsService() throws Exception {
        inspectServices();
    }

    @Test
    void applyingPluginsInMultipleProjectsDoesNotDuplicateServices() throws Exception {
        writeSettingsWithIncludes("services-multi", "core", "geo");
        FilesSupport.write(projectPath("core/build.gradle"), "plugins { id 'ch.so.agi.gretl' }\n" + inspector("core"));
        FilesSupport.write(projectPath("geo/build.gradle"), "plugins { id 'ch.so.agi.gretl.geotools' }\n" + inspector("geo"));
        BuildResult result = run("core:inspect", "geo:inspect", "--parallel", "--max-workers=4");
        assertTrue(result.getOutput().contains("SERVICES_core=[gretlCoreService, gretlGeoToolsService, gretlInterlisService]"));
        assertTrue(result.getOutput().contains("SERVICES_geo=[gretlCoreService, gretlGeoToolsService, gretlInterlisService]"));
    }

    @Test
    void repeatedBuildInSameTestKitDirectoryDoesNotFailServiceRegistration() throws Exception {
        configureServiceBuild();
        BuildResult first = run("inspectServices");
        BuildResult second = run("inspectServices");
        assertTrue(first.getOutput().contains("SERVICES_OK"));
        assertTrue(second.getOutput().contains("SERVICES_OK"));
    }

    private void inspectServices() throws Exception {
        configureServiceBuild();
        BuildResult result = run("inspectServices");
        assertTrue(result.getOutput().contains("SERVICES_OK"), result.getOutput());
    }

    private void configureServiceBuild() throws Exception {
        writeSettings();
        writeGroovyBuild("""
                plugins {
                    id 'ch.so.agi.gretl'
                    id 'ch.so.agi.gretl.geotools'
                }
                import ch.so.agi.gretl.tasks.Gzip
                import ch.so.agi.gretl.geotools.tasks.RasterReclassify
                tasks.register('coreCanary', Gzip)
                tasks.register('geoCanary', RasterReclassify)
                tasks.register('inspectServices') {
                    doLast {
                        def services = gradle.sharedServices.registrations*.name.sort()
                        assert services == ['gretlCoreService', 'gretlGeoToolsService', 'gretlInterlisService']
                        assert tasks.named('coreCanary').get().coreService.isPresent()
                        assert tasks.named('geoCanary').get().geoToolsService.isPresent()
                        println 'SERVICES_OK'
                    }
                }
                """);
    }

    private String inspector(String name) {
        return "tasks.register('inspect') { doLast { println 'SERVICES_" + name
                + "=' + project.gradle.sharedServices.registrations*.name.sort() } }\n";
    }

    private static final class FilesSupport {
        private static void write(java.nio.file.Path path, String content) throws java.io.IOException {
            java.nio.file.Files.createDirectories(path.getParent());
            java.nio.file.Files.writeString(path, content);
        }
    }
}
