package ch.so.agi.gretl.lsp.symbol;

import ch.so.agi.gretl.lsp.metadata.GretlMetadata;
import ch.so.agi.gretl.lsp.metadata.TaskMetadata;
import ch.so.agi.gretl.lsp.model.GretlDslCall;
import ch.so.agi.gretl.lsp.model.GretlScript;
import ch.so.agi.gretl.lsp.model.GretlTaskBlock;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class DocumentSymbolProvider {

    private final GretlMetadata metadata;

    public DocumentSymbolProvider(GretlMetadata metadata) {
        this.metadata = metadata;
    }

    public List<Either<SymbolInformation, DocumentSymbol>> symbols(GretlScript script) {
        List<Either<SymbolInformation, DocumentSymbol>> result = new ArrayList<>();
        for (GretlTaskBlock task : script.tasks()) {
            result.add(Either.forRight(createTaskSymbol(task)));
        }
        return result;
    }

    private DocumentSymbol createTaskSymbol(GretlTaskBlock task) {
        Optional<TaskMetadata> taskMeta = metadata.findTask(task.typeName().orElse(""));
        String typeName = task.typeName().orElse("unknown");

        String symbolName = task.name() + " : " + typeName;
        Range fullRange = task.fullRange() != null ? task.fullRange() : new Range();
        Range selectionRange = task.nameRange() != null ? task.nameRange() : fullRange;

        SymbolKind kind = taskMeta.isPresent()
                ? (taskMeta.get().category() != null && taskMeta.get().category().equals("database")
                        ? SymbolKind.Interface : SymbolKind.Function)
                : SymbolKind.Function;

        List<DocumentSymbol> children = new ArrayList<>();
        for (GretlDslCall call : task.calls()) {
            children.add(createCallSymbol(call));
        }

        return new DocumentSymbol(symbolName, kind, fullRange, selectionRange,
                null, children);
    }

    private DocumentSymbol createCallSymbol(GretlDslCall call) {
        String name = call.name();
        Range fullRange = call.fullRange() != null ? call.fullRange() : new Range();
        Range selectionRange = call.nameRange() != null ? call.nameRange() : fullRange;
        return new DocumentSymbol(name, SymbolKind.Property, fullRange, selectionRange);
    }
}
