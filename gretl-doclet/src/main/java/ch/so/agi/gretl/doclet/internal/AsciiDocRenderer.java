package ch.so.agi.gretl.doclet.internal;

import ch.so.agi.gretl.doclet.model.DslMethodDescriptor;
import ch.so.agi.gretl.doclet.model.TaskDescriptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class AsciiDocRenderer {
    private final MethodSignatureRenderer signatures = new MethodSignatureRenderer();
    private final Messages messages;

    public AsciiDocRenderer(Locale locale) {
        this.messages = new Messages(locale);
    }

    public void render(Path outputDirectory, List<TaskDescriptor> tasks) throws IOException {
        Files.createDirectories(outputDirectory);
        List<TaskDescriptor> sortedTasks = tasks.stream()
                .sorted(Comparator.comparing(TaskDescriptor::name))
                .toList();
        for (TaskDescriptor task : sortedTasks) {
            Files.writeString(outputDirectory.resolve(fileName(task)), renderTask(task), StandardCharsets.UTF_8);
        }
        Files.writeString(outputDirectory.resolve("task-reference.adoc"), renderIndex(sortedTasks), StandardCharsets.UTF_8);
    }

    public String renderTask(TaskDescriptor task) {
        StringBuilder out = new StringBuilder();
        out.append("[[").append(anchor(task)).append("]]\n");
        out.append("== ").append(task.name()).append("\n\n");
        if (!task.description().isBlank()) {
            out.append(task.description()).append("\n\n");
        }
        out.append("[cols=\"4,1,1,5\", options=\"header\"]\n");
        out.append("|===\n");
        out.append("| ").append(messages.dslMethod())
                .append(" | ").append(messages.required())
                .append(" | ").append(messages.defaultColumn())
                .append(" | ").append(messages.description()).append("\n\n");
        for (DslMethodDescriptor method : task.methods()) {
            out.append("| `").append(escapeInline(signatures.render(method))).append("`\n");
            out.append("| ").append(method.required() ? messages.yes() : messages.no()).append("\n");
            out.append("| ").append(method.defaultValue().isBlank() ? "" : escapeCell(method.defaultValue())).append("\n");
            out.append("| ").append(method.description().isBlank() ? "" : escapeCell(method.description())).append("\n\n");
        }
        out.append("|===\n");
        return out.toString();
    }

    private String renderIndex(List<TaskDescriptor> tasks) {
        StringBuilder out = new StringBuilder("= ").append(messages.taskReference()).append("\n\n");
        for (TaskDescriptor task : tasks) {
            out.append("include::").append(fileName(task)).append("[]\n\n");
        }
        return out.toString();
    }

    public String fileName(TaskDescriptor task) {
        return anchor(task) + ".adoc";
    }

    private String anchor(TaskDescriptor task) {
        return "task-" + task.name()
                .replaceAll("[^A-Za-z0-9]+", "-")
                .replaceAll("^-|-$", "")
                .toLowerCase();
    }

    private static String escapeInline(String value) {
        return value.replace("`", "\\`");
    }

    private static String escapeCell(String value) {
        return value.replace("|", "\\|").replace("\r\n", "\n").replace("\n", " +\n");
    }
}
