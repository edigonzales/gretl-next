package ch.so.agi.gretl.lsp.hover;

import ch.so.agi.gretl.lsp.metadata.AcceptedForm;
import ch.so.agi.gretl.lsp.metadata.GretlMetadata;
import ch.so.agi.gretl.lsp.metadata.PropertyMetadata;
import ch.so.agi.gretl.lsp.metadata.TaskMetadata;
import ch.so.agi.gretl.lsp.model.GretlDslCall;
import ch.so.agi.gretl.lsp.model.GretlScript;
import ch.so.agi.gretl.lsp.model.GretlTaskBlock;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import java.util.Optional;

public final class HoverProvider {

    private final GretlMetadata metadata;

    public HoverProvider(GretlMetadata metadata) {
        this.metadata = metadata;
    }

    public Optional<Hover> hover(GretlScript script, Position position) {
        for (GretlTaskBlock block : script.tasks()) {
            if (block.typeName().isPresent() && block.typeRange() != null
                    && inside(position, block.typeRange())) {
                return taskTypeHover(block.typeName().get(), block.typeRange());
            }

            if (block.typeName().isPresent() && block.bodyRange() != null
                    && inside(position, block.bodyRange())) {
                Optional<TaskMetadata> taskMeta = metadata.findTask(block.typeName().get());
                if (taskMeta.isPresent()) {
                    for (GretlDslCall call : block.calls()) {
                        if (inside(position, call.nameRange())) {
                            return propertyHover(taskMeta.get(), call.name());
                        }
                    }
                }
            }
        }

        return Optional.empty();
    }

    private Optional<Hover> taskTypeHover(String typeName, Range typeRange) {
        Optional<TaskMetadata> taskMeta = metadata.findTask(typeName);
        if (taskMeta.isEmpty()) {
            return Optional.empty();
        }

        TaskMetadata task = taskMeta.get();
        StringBuilder sb = new StringBuilder();
        sb.append("**").append(task.name()).append("**\n");
        sb.append("Klasse: `").append(task.qualifiedClassName()).append("`\n");
        sb.append("Kategorie: ").append(task.category() != null ? task.category() : "-").append("\n");
        sb.append("Status: ").append(task.status() != null ? task.status() : "-").append("\n\n");
        sb.append(task.description());

        if (!task.requiredProperties().isEmpty()) {
            sb.append("\n\n**Pflichtfelder:**\n");
            for (PropertyMetadata prop : task.requiredProperties()) {
                sb.append("- `").append(prop.name()).append("`\n");
            }
        }

        if (!task.properties().isEmpty()) {
            sb.append("\n\n**Properties:**\n");
            for (PropertyMetadata prop : task.properties()) {
                String type = prop.valueType() != null ? prop.valueType() : "-";
                String status = prop.required() ? "Pflicht" : "Optional";
                sb.append("- `").append(prop.name()).append("` \u2014 ").append(type)
                        .append(" (").append(status).append(")\n");
            }
        }

        MarkupContent content = new MarkupContent("markdown", sb.toString());
        return Optional.of(new Hover(content, typeRange));
    }

    private Optional<Hover> propertyHover(TaskMetadata taskMeta, String propertyName) {
        Optional<PropertyMetadata> propOpt = taskMeta.findProperty(propertyName);
        if (propOpt.isEmpty()) {
            return Optional.empty();
        }

        PropertyMetadata prop = propOpt.get();
        StringBuilder sb = new StringBuilder();
        sb.append("**").append(prop.name()).append("**  \n");
        sb.append("Typ: `").append(prop.javaType() != null ? prop.javaType() : prop.valueType()).append("`  \n");
        sb.append("Pflicht: ").append(prop.required() ? "ja" : "nein").append("  \n");
        if (prop.deprecated()) {
            sb.append("Status: deprecated  \n");
        }
        if (prop.description() != null) {
            sb.append("\n").append(prop.description());
        }
        if (prop.sqlParameterProvider()) {
            sb.append("\n\nSQL-Parameter-Provider.");
        }

        for (AcceptedForm form : prop.acceptedForms()) {
            if (!form.legacy()) {
                sb.append("\n\n```groovy\n").append(form.signature()).append("\n```");
            }
        }

        MarkupContent content = new MarkupContent("markdown", sb.toString());
        return Optional.of(new Hover(content));
    }

    static boolean inside(Position pos, Range range) {
        if (range == null) {
            return false;
        }
        if (pos.getLine() < range.getStart().getLine()) {
            return false;
        }
        if (pos.getLine() > range.getEnd().getLine()) {
            return false;
        }
        if (pos.getLine() == range.getStart().getLine()
                && pos.getCharacter() < range.getStart().getCharacter()) {
            return false;
        }
        if (pos.getLine() == range.getEnd().getLine()
                && pos.getCharacter() > range.getEnd().getCharacter()) {
            return false;
        }
        return true;
    }
}
