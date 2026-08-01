package ch.so.agi.gretl.combined;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CombinedPluginIndependenceFunctionalTest extends CombinedPluginTestSupport {

    @Test
    void geoToolsPluginDoesNotImplicitlyApplyCorePlugin() throws Exception {
        inspectSinglePlugin("ch.so.agi.gretl.geotools", false);
    }

    @Test
    void corePluginDoesNotImplicitlyApplyGeoToolsPlugin() throws Exception {
        inspectSinglePlugin("ch.so.agi.gretl", false);
    }

    @Test
    void coreOnlyProjectDoesNotRegisterGeoToolsTasks() throws Exception {
        writeSettings();
        writeGroovyBuild("""
                plugins { id 'ch.so.agi.gretl' }
                tasks.register('inspect') {
                    doLast {
                        assert !pluginManager.hasPlugin('ch.so.agi.gretl.geotools')
                        assert tasks.findByName('readShapefile') == null
                        assert extensions.findByName('gretlGeotools') == null
                        println 'CORE_ONLY=OK'
                    }
                }
                """);
        BuildResult result = run("inspect");
        assertTrue(result.getOutput().contains("CORE_ONLY=OK"));
    }

    @Test
    void geoToolsOnlyProjectDoesNotRegisterCoreTasks() throws Exception {
        writeSettings();
        writeGroovyBuild("""
                plugins { id 'ch.so.agi.gretl.geotools' }
                tasks.register('inspect') {
                    doLast {
                        assert !pluginManager.hasPlugin('ch.so.agi.gretl')
                        assert tasks.findByName('readShapefile') != null
                        assert gradle.sharedServices.registrations*.name == ['gretlGeoToolsService']
                        println 'GEOTOOLS_ONLY=OK'
                    }
                }
                """);
        BuildResult result = run("inspect");
        assertTrue(result.getOutput().contains("GEOTOOLS_ONLY=OK"));
    }

    @Test
    void combinedProjectRequiresBothExplicitPluginDeclarations() throws Exception {
        writeSettings();
        writeGroovyBuild("""
                plugins { id 'ch.so.agi.gretl' }
                tasks.register('inspect') {
                    doLast { assert !pluginManager.hasPlugin('ch.so.agi.gretl.geotools') }
                }
                """);
        BuildResult result = run("inspect");
        assertFalse(result.getOutput().contains("gretl.geotools"));
    }

    private void inspectSinglePlugin(String pluginId, boolean expectedOther) throws Exception {
        writeSettings();
        writeGroovyBuild("""
                plugins { id '%s' }
                tasks.register('inspect') {
                    doLast {
                        assert pluginManager.hasPlugin('%s')
                        assert pluginManager.hasPlugin('ch.so.agi.gretl') == %s
                        assert pluginManager.hasPlugin('ch.so.agi.gretl.geotools') == %s
                        println 'PLUGIN_INDEPENDENCE=OK'
                    }
                }
                """.formatted(pluginId, pluginId,
                pluginId.equals("ch.so.agi.gretl"),
                pluginId.equals("ch.so.agi.gretl.geotools")));
        BuildResult result = run("inspect");
        assertTrue(result.getOutput().contains("PLUGIN_INDEPENDENCE=OK"));
    }
}
