package ch.so.agi.gretl.doclet.lsp;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class LspMetadataWriter {

    private static final String INDENT = "  ";

    public void write(LspMetadataDocument document, Path outputFile) throws IOException {
        StringBuilder out = new StringBuilder();
        writeDocument(out, document, 0);
        Files.createDirectories(outputFile.getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
            writer.write(out.toString());
            writer.write('\n');
        }
    }

    private void writeDocument(StringBuilder out, LspMetadataDocument doc, int depth) {
        out.append("{\n");
        String in = INDENT.repeat(depth + 1);
        writeStringField(out, in, "schemaVersion", doc.schemaVersion());
        writeStringField(out, in, "generatedAt", doc.generatedAt());
        writeStringField(out, in, "gretlVersion", doc.gretlVersion());
        writeSource(out, in, doc.source());
        writeTasksArray(out, in, doc.tasks());
        removeTrailingComma(out);
        out.append('\n').append(INDENT.repeat(depth)).append("}");
    }

    private void writeSource(StringBuilder out, String in, LspMetadataSource source) {
        out.append(in).append("\"source\": {\n");
        String inner = in + INDENT;
        writeStringField(out, inner, "repository", source.repository());
        writeStringField(out, inner, "doclet", source.doclet());
        if (source.commit() != null) {
            writeStringField(out, inner, "commit", source.commit());
        }
        removeTrailingComma(out);
        out.append('\n').append(in).append("},\n");
    }

    private void writeTasksArray(StringBuilder out, String in, List<LspTaskMetadata> tasks) {
        out.append(in).append("\"tasks\": [\n");
        for (int i = 0; i < tasks.size(); i++) {
            writeTask(out, in + INDENT, tasks.get(i));
            if (i < tasks.size() - 1) {
                out.append(",\n");
            } else {
                out.append("\n");
            }
        }
        out.append(in).append("]\n");
    }

    private void writeTask(StringBuilder out, String in, LspTaskMetadata task) {
        out.append(in).append("{\n");
        String fi = in + INDENT;
        writeStringField(out, fi, "name", task.name());
        writeStringField(out, fi, "qualifiedClassName", task.qualifiedClassName());
        writeStringField(out, fi, "simpleClassName", task.simpleClassName());
        writeStringField(out, fi, "category", task.category());
        writeStringField(out, fi, "status", task.status());
        writeStringField(out, fi, "description", task.description());
        if (task.longDescription() != null) {
            writeStringField(out, fi, "longDescription", task.longDescription());
        }
        writeExamplesArray(out, fi, task.examples());
        writePropertiesArray(out, fi, task.properties());
        removeTrailingComma(out);
        out.append('\n').append(in).append("}");
    }

    private void writeExamplesArray(StringBuilder out, String in, List<LspExample> examples) {
        if (examples == null || examples.isEmpty()) {
            out.append(in).append("\"examples\": [],\n");
            return;
        }
        out.append(in).append("\"examples\": [\n");
        for (int i = 0; i < examples.size(); i++) {
            writeExample(out, in + INDENT, examples.get(i));
            if (i < examples.size() - 1) {
                out.append(",\n");
            } else {
                out.append("\n");
            }
        }
        out.append(in).append("],\n");
    }

    private void writeExample(StringBuilder out, String in, LspExample example) {
        out.append(in).append("{\n");
        String fi = in + INDENT;
        writeStringField(out, fi, "title", example.title());
        writeStringField(out, fi, "language", example.language());
        writeStringField(out, fi, "body", example.body());
        removeTrailingComma(out);
        out.append('\n').append(in).append("}");
    }

    private void writePropertiesArray(StringBuilder out, String in, List<LspPropertyMetadata> properties) {
        out.append(in).append("\"properties\": [\n");
        for (int i = 0; i < properties.size(); i++) {
            writeProperty(out, in + INDENT, properties.get(i));
            if (i < properties.size() - 1) {
                out.append(",\n");
            } else {
                out.append("\n");
            }
        }
        out.append(in).append("]\n");
    }

    private void writeProperty(StringBuilder out, String in, LspPropertyMetadata prop) {
        out.append(in).append("{\n");
        String fi = in + INDENT;
        writeStringField(out, fi, "name", prop.name());
        writeStringField(out, fi, "displayName", prop.displayName());
        writeStringField(out, fi, "kind", prop.kind());
        writeStringField(out, fi, "valueType", prop.valueType());
        writeStringField(out, fi, "javaType", prop.javaType());
        writeBooleanField(out, fi, "required", prop.required());
        writeBooleanField(out, fi, "deprecated", prop.deprecated());
        writeStringField(out, fi, "description", prop.description());
        if (prop.file() != null) {
            writeFileMetadata(out, fi, prop.file());
        }
        writeAcceptedFormsArray(out, fi, prop.acceptedForms());
        if (prop.migration() != null) {
            writeMigration(out, fi, prop.migration());
        }
        writeBooleanField(out, fi, "sqlParameterProvider", prop.sqlParameterProvider());
        if (prop.completion() != null) {
            writeCompletion(out, fi, prop.completion());
        }
        removeTrailingComma(out);
        out.append('\n').append(in).append("}");
    }

    private void writeFileMetadata(StringBuilder out, String in, LspFileMetadata file) {
        out.append(in).append("\"file\": {\n");
        String fi = in + INDENT;
        writeStringField(out, fi, "role", file.role());
        writeStringListField(out, fi, "extensions", file.extensions());
        writeBooleanField(out, fi, "multiple", file.multiple());
        writeBooleanField(out, fi, "mustExist", file.mustExist());
        removeTrailingComma(out);
        out.append('\n').append(in).append("},\n");
    }

    private void writeAcceptedFormsArray(StringBuilder out, String in, List<LspAcceptedForm> forms) {
        out.append(in).append("\"acceptedForms\": [\n");
        for (int i = 0; i < forms.size(); i++) {
            writeAcceptedForm(out, in + INDENT, forms.get(i));
            if (i < forms.size() - 1) {
                out.append(",\n");
            } else {
                out.append("\n");
            }
        }
        out.append(in).append("],\n");
    }

    private void writeAcceptedForm(StringBuilder out, String in, LspAcceptedForm form) {
        out.append(in).append("{\n");
        String fi = in + INDENT;
        writeStringField(out, fi, "style", form.style());
        writeStringField(out, fi, "signature", form.signature());
        writeStringField(out, fi, "insertText", form.insertText());
        if (form.argumentCount() != null) {
            writeIntField(out, fi, "argumentCount", form.argumentCount());
        }
        writeBooleanField(out, fi, "legacy", form.legacy());
        removeTrailingComma(out);
        out.append('\n').append(in).append("}");
    }

    private void writeMigration(StringBuilder out, String in, LspMigrationMetadata migration) {
        out.append(in).append("\"migration\": {\n");
        String fi = in + INDENT;
        writeStringListField(out, fi, "from", migration.from());
        writeStringField(out, fi, "to", migration.to());
        writeStringField(out, fi, "codeActionTitle", migration.codeActionTitle());
        removeTrailingComma(out);
        out.append('\n').append(in).append("},\n");
    }

    private void writeCompletion(StringBuilder out, String in, LspCompletionMetadata completion) {
        out.append(in).append("\"completion\": {\n");
        String fi = in + INDENT;
        writeStringField(out, fi, "label", completion.label());
        writeStringField(out, fi, "detail", completion.detail());
        writeStringField(out, fi, "sortText", completion.sortText());
        removeTrailingComma(out);
        out.append('\n').append(in).append("},\n");
    }

    private void writeStringField(StringBuilder out, String in, String key, String value) {
        if (value == null) return;
        out.append(in).append("\"").append(escapeJson(key)).append("\": \"")
                .append(escapeJson(value)).append("\",\n");
    }

    private void writeBooleanField(StringBuilder out, String in, String key, boolean value) {
        out.append(in).append("\"").append(escapeJson(key)).append("\": ").append(value).append(",\n");
    }

    private void writeIntField(StringBuilder out, String in, String key, int value) {
        out.append(in).append("\"").append(escapeJson(key)).append("\": ").append(value).append(",\n");
    }

    private void writeStringListField(StringBuilder out, String in, String key, List<String> values) {
        out.append(in).append("\"").append(escapeJson(key)).append("\": [");
        for (int i = 0; i < values.size(); i++) {
            out.append("\"").append(escapeJson(values.get(i))).append("\"");
            if (i < values.size() - 1) out.append(", ");
        }
        out.append("],\n");
    }

    private static void removeTrailingComma(StringBuilder out) {
        int lastNonWhitespace = out.length() - 1;
        while (lastNonWhitespace >= 0 && Character.isWhitespace(out.charAt(lastNonWhitespace))) {
            lastNonWhitespace--;
        }
        if (lastNonWhitespace >= 0 && out.charAt(lastNonWhitespace) == ',') {
            out.deleteCharAt(lastNonWhitespace);
        }
    }

    static String escapeJson(String value) {
        if (value == null) return "";
        StringBuilder sb = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}
