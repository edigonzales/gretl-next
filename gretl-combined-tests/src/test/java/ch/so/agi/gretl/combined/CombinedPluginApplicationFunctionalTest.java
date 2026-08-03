package ch.so.agi.gretl.combined;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CombinedPluginApplicationFunctionalTest extends CombinedPluginTestSupport {

    @Test
    void appliesCoreThenGeoToolsWithGroovyDsl() throws Exception {
        inspectGroovy("ch.so.agi.gretl", "ch.so.agi.gretl.geotools");
    }

    @Test
    void appliesGeoToolsThenCoreWithGroovyDsl() throws Exception {
        inspectGroovy("ch.so.agi.gretl.geotools", "ch.so.agi.gretl");
    }

    @Test
    void bothPluginsExposeTheirTaskTypes() throws Exception {
        inspectGroovy("ch.so.agi.gretl", "ch.so.agi.gretl.geotools");
    }

    @Test
    void bothPluginsCanBeAppliedAgainThroughPluginManagerIdempotently() throws Exception {
        writeSettings();
        writeGroovyBuild("""
                plugins {
                    id 'ch.so.agi.gretl'
                    id 'ch.so.agi.gretl.geotools'
                }
                pluginManager.apply('ch.so.agi.gretl')
                pluginManager.apply('ch.so.agi.gretl.geotools')
                tasks.register('inspectCombined') {
                    doLast {
                        assert project.tasks.names.count { it == 'readShapefile' } == 1
                        assert project.extensions.findByName('gretlGeotools') != null
                        println 'IDEMPOTENT_COMBINED_PLUGIN=OK'
                    }
                }
                """);
        BuildResult result = run("inspectCombined");
        assertTrue(result.getOutput().contains("IDEMPOTENT_COMBINED_PLUGIN=OK"));
    }

    @Test
    void applyingBothDoesNotRegisterDuplicateTasks() throws Exception {
        inspectGroovy("ch.so.agi.gretl", "ch.so.agi.gretl.geotools");
    }

    @Test
    void applyingBothDoesNotRegisterDuplicateExtensions() throws Exception {
        inspectGroovy("ch.so.agi.gretl", "ch.so.agi.gretl.geotools");
    }

    @Test
    void applyingBothDoesNotRegisterConflictingServices() throws Exception {
        inspectGroovy("ch.so.agi.gretl", "ch.so.agi.gretl.geotools");
    }

    private void inspectGroovy(String first, String second) throws Exception {
        writeSettings();
        writeGroovyBuild("""
                plugins {
                    id '%s'
                    id '%s'
                }
                import ch.so.agi.gretl.tasks.Gzip
                import ch.so.agi.gretl.tasks.XslTransformer
                import ch.so.agi.gretl.geotools.tasks.RasterReclassify
                tasks.register('gzipCanary', Gzip)
                tasks.register('xslCanary', XslTransformer)
                tasks.register('rasterCanary', RasterReclassify)
                tasks.register('inspectCombined') {
                    doLast {
                        assert project.pluginManager.hasPlugin('ch.so.agi.gretl')
                        assert project.pluginManager.hasPlugin('ch.so.agi.gretl.geotools')
                        assert project.tasks.named('gzipCanary').get() instanceof Gzip
                        assert project.tasks.named('xslCanary').get() instanceof XslTransformer
                        assert project.tasks.named('rasterCanary').get() instanceof RasterReclassify
                        assert project.extensions.findByName('gretlGeotools') != null
                        def names = project.gradle.sharedServices.registrations*.name
                        assert names.count { it == 'gretlCoreService' } == 1
                        assert names.count { it == 'gretlGeoToolsService' } == 1
                        println 'COMBINED_PLUGIN_INSPECTION=OK'
                    }
                }
                """.formatted(first, second));
        assertInspection(run("inspectCombined"));
    }

    private void assertInspection(BuildResult result) {
        assertTrue(result.getOutput().contains("COMBINED_PLUGIN_INSPECTION=OK"), result.getOutput());
        assertNoCombinedPluginWarnings(result);
    }
}
