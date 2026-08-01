package ch.so.agi.gretl.test.job;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class TestJobDescriptorValidator {
    private static final Pattern ID = Pattern.compile("[a-z][a-z0-9]*(?:-[a-z0-9]+)*");
    private static final Pattern CLASS = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*(?:\\.[A-Za-z_$][A-Za-z0-9_$]*)+");
    private static final Set<String> CATEGORIES = Set.of("core", "geotools", "combined", "database", "network", "validator");
    private static final Set<String> FORBIDDEN = Set.of("includeBuild", "mavenLocal()", "flatDir", "withPluginClasspath",
            "gretl-core/build", "gretl-geotools/build", "build/classes", "build/resources", "apply plugin:");

    public void validate(TestJobDescriptor descriptor) {
        List<TestJobValidationError> errors = errors(descriptor);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Invalid test job descriptor '" + descriptor.sourceDirectory()
                    + "': " + errors);
        }
    }

    public List<TestJobValidationError> errors(TestJobDescriptor descriptor) {
        List<TestJobValidationError> errors = new ArrayList<>();
        if (descriptor == null) {
            return List.of(new TestJobValidationError("descriptor", "must not be null"));
        }
        if (descriptor.schemaVersion() != 1) add(errors, "schemaVersion", "must be 1");
        if (!ID.matcher(descriptor.id()).matches()) add(errors, "id", "must match " + ID.pattern());
        if (descriptor.description().isBlank()) add(errors, "description", "must not be blank");
        if (!CATEGORIES.contains(descriptor.category())) add(errors, "category", "unsupported category");
        if (descriptor.builds().isEmpty()) add(errors, "builds", "at least one build variant is required");
        Set<String> buildIds = new HashSet<>();
        Set<String> buildFiles = new HashSet<>();
        for (int i = 0; i < descriptor.builds().size(); i++) {
            TestJobBuildVariant build = descriptor.builds().get(i);
            String field = "builds[" + i + "]";
            if (!buildIds.add(build.id())) add(errors, field + ".id", "must be unique");
            if (!buildFiles.add(build.file())) add(errors, field + ".file", "must be unique");
            Path source = resolve(descriptor.sourceDirectory(), build.file());
            if (source == null || !Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS)) {
                add(errors, field + ".file", "does not exist inside the job directory");
            } else {
                String expectedSuffix = build.language() == TestJobBuildLanguage.GROOVY ? ".gradle" : ".gradle.kts";
                if (!build.file().endsWith(expectedSuffix)) add(errors, field + ".file", "does not match its language");
                inspectBuild(source, field + ".file", errors);
            }
        }
        if (descriptor.entryTasks().isEmpty()) add(errors, "entryTasks", "at least one task is required");
        descriptor.entryTasks().forEach((task) -> {
            if (task.startsWith("-") || task.isBlank()) add(errors, "entryTasks", "contains an invalid task: " + task);
        });
        if (descriptor.expectedTasks().isEmpty()) add(errors, "expectedTasks", "at least one task is required");
        Set<String> expectedPaths = new HashSet<>();
        for (int i = 0; i < descriptor.expectedTasks().size(); i++) {
            ExpectedTaskExecution task = descriptor.expectedTasks().get(i);
            if (!task.path().startsWith(":")) add(errors, "expectedTasks[" + i + "].path", "must begin with ':'");
            if (!expectedPaths.add(task.path())) add(errors, "expectedTasks[" + i + "].path", "must be unique");
            if (!CLASS.matcher(task.className()).matches()) add(errors, "expectedTasks[" + i + "].className", "must be fully qualified");
        }
        for (TestJobExecutionTarget target : TestJobExecutionTarget.values()) {
            if (!descriptor.executionTargets().containsKey(target)) add(errors, "executionTargets." + target.yamlName(), "is required");
        }
        if (descriptor.assertions().isBlank()) add(errors, "assertions", "must not be blank");
        Duration timeout = descriptor.timeout();
        if (timeout.isNegative() || timeout.isZero() || timeout.getSeconds() < 10 || timeout.getSeconds() > 1800) {
            add(errors, "timeoutSeconds", "must be between 10 and 1800");
        }
        inspectCatalogFiles(descriptor.sourceDirectory(), errors);
        return List.copyOf(errors);
    }

    private void inspectBuild(Path file, String field, List<TestJobValidationError> errors) {
        try {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            FORBIDDEN.stream().filter(text::contains).forEach(value -> add(errors, field, "contains forbidden consumer construct '" + value + "'"));
        } catch (IOException e) {
            add(errors, field, "cannot be read: " + e.getMessage());
        }
    }

    private void inspectCatalogFiles(Path root, List<TestJobValidationError> errors) {
        if (!Files.isDirectory(root)) {
            add(errors, "sourceDirectory", "is not a directory");
            return;
        }
        try (var paths = Files.walk(root)) {
            paths.filter(path -> !path.equals(root)).forEach(path -> {
                Path name = path.getFileName();
                if (name == null) return;
                String value = name.toString();
                if (Files.isDirectory(path) && Set.of("build", ".gradle", ".git", ".gradle-cache").contains(value)) {
                    add(errors, value, "generated directory must not be in the catalog");
                }
                if (value.endsWith(".class") || value.endsWith(".jar") || value.endsWith(".log")) {
                    add(errors, value, "generated output must not be in the catalog");
                }
            });
        } catch (IOException e) {
            add(errors, "sourceDirectory", "cannot be inspected: " + e.getMessage());
        }
    }

    private Path resolve(Path root, String relative) {
        Path candidate;
        try {
            candidate = root.resolve(relative).normalize();
        } catch (RuntimeException e) {
            return null;
        }
        return candidate.startsWith(root) && !Path.of(relative).isAbsolute() ? candidate : null;
    }

    private static void add(List<TestJobValidationError> errors, String field, String message) {
        errors.add(new TestJobValidationError(field, message));
    }
}
