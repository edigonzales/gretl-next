package ch.so.agi.gretl.test.fixture;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public final class AdditionalFixturePlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getTasks().register("fixturePluginTask", task -> task.doLast(action ->
                project.getLayout().getBuildDirectory().file("fixture-plugin.txt").get().getAsFile().toPath()
                        .toFile().getParentFile().mkdirs()));
        project.getTasks().named("fixturePluginTask").configure(task -> task.doLast(action -> {
            try {
                java.nio.file.Files.writeString(
                        project.getLayout().getBuildDirectory().file("fixture-plugin.txt").get().getAsFile().toPath(),
                        "additional-plugin");
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
        }));
    }
}
