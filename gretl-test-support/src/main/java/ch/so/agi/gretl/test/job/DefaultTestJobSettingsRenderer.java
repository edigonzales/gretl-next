package ch.so.agi.gretl.test.job;

import ch.so.agi.gretl.testkit.GretlTestProjectSettings;

public final class DefaultTestJobSettingsRenderer implements TestJobSettingsRenderer {
    @Override
    public String renderGroovy(TestJobSettingsRequest request) {
        String projectName = escape(request.projectName());
        return switch (request.target()) {
            case PLUGIN_CLASSPATH -> "rootProject.name = '" + projectName + "'\n";
            case PUBLISHED_ARTIFACT -> {
                if (request.publishedRepository().isEmpty() || request.pluginVersion().isEmpty()) {
                    throw new IllegalArgumentException("Published settings require repository and pluginVersion");
                }
                yield GretlTestProjectSettings.renderPublished(
                        request.projectName(), request.publishedRepository().get(), request.pluginVersion().get());
            }
            case RUNTIME_IMAGE_ONE_SHOT, RUNTIME_IMAGE_SERVICE ->
                    "rootProject.name = '" + projectName + "'\n";
        };
    }

    private static String escape(String value) {
        if (value.isBlank()) throw new IllegalArgumentException("projectName must not be blank");
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }
}
