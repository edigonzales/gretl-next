package ch.so.agi.gretl.lsp.completion;

import ch.so.agi.gretl.lsp.metadata.AcceptedForm;
import ch.so.agi.gretl.lsp.metadata.GretlMetadata;
import ch.so.agi.gretl.lsp.metadata.PropertyMetadata;
import ch.so.agi.gretl.lsp.metadata.TaskMetadata;
import ch.so.agi.gretl.lsp.model.GretlScript;
import ch.so.agi.gretl.lsp.model.GretlTaskBlock;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.InsertTextFormat;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CompletionProvider {

    private final GretlMetadata metadata;
    private final CompletionContextDetector contextDetector;

    public CompletionProvider(GretlMetadata metadata) {
        this.metadata = metadata;
        this.contextDetector = new CompletionContextDetector();
    }

    public Either<List<CompletionItem>, CompletionList> complete(
            GretlScript script, Position position, String currentLineText) {
        CompletionContext context = contextDetector.detect(script, position, currentLineText);

        return switch (context.kind()) {
            case TASK_TYPE -> taskTypeCompletion(position, currentLineText);
            case INSIDE_GRETL_TASK_BODY -> propertyCompletion(context.taskBlock());
            case DEPENDENCY_TASK_NAME -> dependencyCompletion(script);
            case IMPORT -> importCompletion(context.importPrefix(), position, currentLineText);
            case TOP_LEVEL, FILE_PATH, SQL_PARAMETER_NAME, UNKNOWN -> Either.forLeft(List.of());
        };
    }

    private Either<List<CompletionItem>, CompletionList> taskTypeCompletion(
            Position position, String currentLineText) {
        int col = Math.min(position.getCharacter(),
                currentLineText != null ? currentLineText.length() : 0);
        String before = currentLineText != null ? currentLineText.substring(0, col) : "";
        int commaIdx = before.lastIndexOf(',');
        int replaceStart = commaIdx >= 0 ? commaIdx + 1 : col;
        boolean afterComma = commaIdx >= 0;

        List<CompletionItem> items = new ArrayList<>();
        for (TaskMetadata task : metadata.tasksSortedByName()) {
            CompletionItem item = new CompletionItem(task.name());
            item.setKind(CompletionItemKind.Class);
            item.setDetail(categoryLabel(task.category()) + " | " + task.status());
            item.setDocumentation(toMarkup(task.description()));

            Range editRange = new Range(
                    new Position(position.getLine(), replaceStart),
                    position);
            String insertText = afterComma ? " " + task.name() : task.name();
            item.setTextEdit(Either.forLeft(new TextEdit(editRange, insertText)));

            items.add(item);
        }
        return Either.forLeft(items);
    }

    private Either<List<CompletionItem>, CompletionList> propertyCompletion(GretlTaskBlock taskBlock) {
        if (taskBlock == null) {
            return Either.forLeft(List.of());
        }
        if (taskBlock.typeName().isEmpty()) {
            return Either.forLeft(List.of());
        }

        Optional<TaskMetadata> taskMeta = metadata.findTask(taskBlock.typeName().get());
        if (taskMeta.isEmpty()) {
            return Either.forLeft(List.of());
        }

        List<CompletionItem> items = new ArrayList<>();
        for (PropertyMetadata prop : taskMeta.get().completionProperties()) {
            if (taskBlock.hasCall(prop.name())) {
                continue;
            }

            CompletionItem item = new CompletionItem(prop.name());
            item.setKind(CompletionItemKind.Property);

            if (prop.completion() != null) {
                if (prop.completion().detail() != null) {
                    item.setDetail(prop.completion().detail());
                }
                if (prop.completion().sortText() != null) {
                    item.setSortText(prop.completion().sortText());
                }
            } else {
                item.setDetail(prop.required() ? "Pflicht" : "Optional");
            }

            item.setDocumentation(toMarkup(prop.description()));

            Optional<AcceptedForm> mainForm = prop.acceptedForms().stream()
                    .filter(f -> !f.legacy() && f.insertText() != null && !f.insertText().isBlank())
                    .findFirst();
            if (mainForm.isPresent()) {
                item.setInsertText(mainForm.get().insertText());
                item.setInsertTextFormat(InsertTextFormat.Snippet);
            }

            items.add(item);
        }

        return Either.forLeft(items);
    }

    private Either<List<CompletionItem>, CompletionList> importCompletion(
            String prefix, Position position, String currentLineText) {
        int col = Math.min(position.getCharacter(),
                currentLineText != null ? currentLineText.length() : 0);
        String before = currentLineText != null ? currentLineText.substring(0, col) : "";
        int importKeywordIdx = before.lastIndexOf("import ");
        int replaceStart = importKeywordIdx >= 0
                ? importKeywordIdx + "import ".length()
                : position.getCharacter();

        List<CompletionItem> items = new ArrayList<>();
        for (TaskMetadata task : metadata.tasksSortedByName()) {
            String fqn = task.qualifiedClassName();
            if (prefix != null && !prefix.isEmpty() && !fqn.startsWith(prefix)) {
                continue;
            }
            CompletionItem item = new CompletionItem(fqn);
            item.setKind(CompletionItemKind.Class);
            item.setDetail(task.name());
            item.setDocumentation(toMarkup(task.description()));

            Range editRange = new Range(
                    new Position(position.getLine(), replaceStart),
                    position);
            item.setTextEdit(Either.forLeft(new TextEdit(editRange, fqn)));

            items.add(item);
        }
        return Either.forLeft(items);
    }

    private Either<List<CompletionItem>, CompletionList> dependencyCompletion(GretlScript script) {
        List<CompletionItem> items = new ArrayList<>();
        for (String name : script.taskNames()) {
            CompletionItem item = new CompletionItem(name);
            item.setKind(CompletionItemKind.Reference);
            Optional<GretlTaskBlock> block = script.taskByName(name);
            block.ifPresent(b -> {
                if (b.typeName().isPresent()) {
                    item.setDetail(b.typeName().get());
                }
            });
            items.add(item);
        }
        return Either.forLeft(items);
    }

    private static String categoryLabel(String category) {
        if (category == null) {
            return "";
        }
        return switch (category) {
            case "database" -> "Database";
            case "filetransfer" -> "File Transfer";
            case "storage" -> "Storage";
            case "interlis" -> "INTERLIS";
            case "transformation" -> "Transformation";
            case "network" -> "Network";
            case "conversion" -> "Conversion";
            default -> "Other";
        };
    }

    private static org.eclipse.lsp4j.MarkupContent toMarkup(String text) {
        if (text == null) {
            return null;
        }
        return new org.eclipse.lsp4j.MarkupContent("markdown", text);
    }
}
