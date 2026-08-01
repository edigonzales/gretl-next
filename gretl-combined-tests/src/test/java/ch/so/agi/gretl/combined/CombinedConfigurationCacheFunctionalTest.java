package ch.so.agi.gretl.combined;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CombinedConfigurationCacheFunctionalTest extends CombinedPluginTestSupport {

    @Test
    void combinedPluginApplicationStoresConfigurationCache() throws Exception {
        configureGroovy("tasks.register('canary') { doLast { println 'CACHE_CANARY' } }");
        BuildResult result = run("canary", "--configuration-cache", "--info");
        assertStoredOrReused(result);
    }

    @Test
    void combinedPipelineReusesConfigurationCache() throws Exception {
        configureGroovy("tasks.register('canary') { doLast { println 'CACHE_CANARY' } }");
        run("canary", "--configuration-cache");
        BuildResult result = run("canary", "--configuration-cache", "--info");
        assertTrue(result.getOutput().contains("Configuration cache entry reused"), result.getOutput());
    }

    @Test
    void groovyPipelineReusesConfigurationCache() throws Exception {
        configureGroovy("tasks.register('canary') { doLast { println 'CACHE_CANARY' } }");
        assertCacheReused();
    }

    @Test
    void kotlinPipelineReusesConfigurationCache() throws Exception {
        writeSettings();
        writeKotlinBuild("""
                plugins {
                    id("ch.so.agi.gretl")
                    id("ch.so.agi.gretl.geotools")
                }
                tasks.register("canary") { doLast { println("CACHE_CANARY") } }
                """);
        assertCacheReused();
    }

    @Test
    void configurationCachePreservesWorkerIsolation() throws Exception {
        writeSettings();
        writeGroovyBuild("""
                plugins {
                    id 'ch.so.agi.gretl'
                    id 'ch.so.agi.gretl.geotools'
                }
                tasks.register('canary') {
                    doLast {
                        assert gradle.sharedServices.registrations*.name.contains('gretlGeoToolsService')
                        println 'WORKER_ISOLATION_CONFIGURATION=OK'
                    }
                }
                """);
        BuildResult result = run("canary", "--configuration-cache");
        assertTrue(result.getOutput().contains("WORKER_ISOLATION_CONFIGURATION=OK"));
    }

    private void configureGroovy(String extra) throws Exception {
        writeSettings();
        writeGroovyBuild("""
                plugins {
                    id 'ch.so.agi.gretl'
                    id 'ch.so.agi.gretl.geotools'
                }
                %s
                """.formatted(extra));
    }

    private void assertStoredOrReused(BuildResult result) {
        assertTrue(result.getOutput().contains("Configuration cache entry stored")
                        || result.getOutput().contains("Configuration cache entry reused"), result.getOutput());
    }

    private void assertCacheReused() {
        run("canary", "--configuration-cache");
        BuildResult result = run("canary", "--configuration-cache", "--info");
        assertTrue(result.getOutput().contains("Configuration cache entry reused"), result.getOutput());
    }
}
