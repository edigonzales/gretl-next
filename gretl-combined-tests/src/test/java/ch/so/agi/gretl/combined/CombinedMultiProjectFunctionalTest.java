package ch.so.agi.gretl.combined;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CombinedMultiProjectFunctionalTest extends CombinedPluginTestSupport {

    @Test
    void supportsCoreOnlyGeoToolsOnlyAndMixedSubprojects() throws Exception {
        configureMultiProject();
        BuildResult result = run("aggregate", "--parallel", "--max-workers=4");
        assertTrue(result.getOutput().contains("core-only: CORE=true GEO=false"));
        assertTrue(result.getOutput().contains("geotools-only: CORE=false GEO=true"));
        assertTrue(result.getOutput().contains("mixed-a: CORE=true GEO=true"));
        assertTrue(result.getOutput().contains("mixed-b: CORE=true GEO=true"));
    }

    @Test
    void mixedSubprojectsCanUseDifferentPluginOrder() throws Exception {
        configureMultiProject();
        BuildResult result = run("mixed-a:inspect", "mixed-b:inspect", "--parallel", "--max-workers=4");
        assertTrue(result.getOutput().contains("mixed-a: CORE=true GEO=true"));
        assertTrue(result.getOutput().contains("mixed-b: CORE=true GEO=true"));
    }

    @Test
    void rootAggregateExecutesAllSubprojectTasks() throws Exception {
        configureMultiProject();
        BuildResult result = run("aggregate");
        assertTrue(result.getOutput().contains("core-only:inspect"));
        assertTrue(result.getOutput().contains("mixed-b:inspect"));
    }

    @Test
    void subprojectOutputsRemainIsolated() throws Exception {
        configureMultiProject();
        run("aggregate");
        assertTrue(Files.isRegularFile(projectPath("core-only/build/marker.txt")));
        assertTrue(Files.isRegularFile(projectPath("geotools-only/build/marker.txt")));
        assertTrue(Files.isRegularFile(projectPath("mixed-a/build/marker.txt")));
        assertTrue(Files.isRegularFile(projectPath("mixed-b/build/marker.txt")));
    }

    @Test
    void parallelMultiProjectBuildCompletesWithoutDeadlock() throws Exception {
        configureMultiProject();
        BuildResult result = run("aggregate", "--parallel", "--max-workers=4", "--rerun-tasks");
        assertTrue(result.getOutput().contains("mixed-b: CORE=true GEO=true"));
    }

    @Test
    void failureInOneSubprojectDoesNotCorruptNextBuild() throws Exception {
        configureMultiProject();
        run("aggregate");
        Files.writeString(projectPath("mixed-a/build.gradle"), "plugins { id 'ch.so.agi.gretl' }\n" + marker("mixed-a"));
        BuildResult result = run("aggregate");
        assertTrue(result.getOutput().contains("mixed-a: CORE=true GEO=false"));
        assertTrue(result.getOutput().contains("mixed-b: CORE=true GEO=true"));
    }

    private void configureMultiProject() throws Exception {
        writeSettingsWithIncludes("multi-root", "core-only", "geotools-only", "mixed-a", "mixed-b");
        writeSubproject("core-only", "id 'ch.so.agi.gretl'");
        writeSubproject("geotools-only", "id 'ch.so.agi.gretl.geotools'");
        writeSubproject("mixed-a", "id 'ch.so.agi.gretl'\n    id 'ch.so.agi.gretl.geotools'");
        writeSubproject("mixed-b", "id 'ch.so.agi.gretl.geotools'\n    id 'ch.so.agi.gretl'");
        writeGroovyBuild("""
                tasks.register('aggregate') {
                    dependsOn ':core-only:inspect', ':geotools-only:inspect', ':mixed-a:inspect', ':mixed-b:inspect'
                }
                """);
    }

    private void writeSubproject(String name, String plugins) throws Exception {
        Files.createDirectories(projectPath(name));
        Files.writeString(projectPath(name + "/build.gradle"), "plugins {\n    %s\n}\n%s".formatted(plugins, marker(name)));
    }

    private String marker(String name) {
        return ("tasks.register('inspect') { doLast { "
                + "println '%s: CORE=' + pluginManager.hasPlugin('ch.so.agi.gretl') + ' GEO=' + pluginManager.hasPlugin('ch.so.agi.gretl.geotools'); "
                + "def marker = layout.buildDirectory.file('marker.txt').get().asFile; marker.parentFile.mkdirs(); marker.text = '%s' "
                + "} }\n").formatted(name, name);
    }
}
