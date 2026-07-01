package ch.so.agi.gretl.lsp.links;

import ch.so.agi.gretl.lsp.metadata.FileMetadata;
import ch.so.agi.gretl.lsp.metadata.GretlMetadata;
import ch.so.agi.gretl.lsp.metadata.PropertyMetadata;
import ch.so.agi.gretl.lsp.metadata.TaskMetadata;
import ch.so.agi.gretl.lsp.model.GretlDslCall;
import ch.so.agi.gretl.lsp.model.GretlScript;
import ch.so.agi.gretl.lsp.model.GretlTaskBlock;
import ch.so.agi.gretl.lsp.util.FileReferenceUtil;
import org.eclipse.lsp4j.DocumentLink;
import org.eclipse.lsp4j.Range;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class DocumentLinkProvider {

    private final GretlMetadata metadata;

    public DocumentLinkProvider(GretlMetadata metadata) {
        this.metadata = metadata;
    }

    public List<DocumentLink> links(GretlScript script, Path workspaceRoot) {
        List<DocumentLink> result = new ArrayList<>();
        if (workspaceRoot == null) {
            return result;
        }

        for (GretlTaskBlock task : script.tasks()) {
            Optional<TaskMetadata> taskMetaOpt = metadata.findTask(task.typeName().orElse(""));
            if (taskMetaOpt.isEmpty()) {
                continue;
            }
            TaskMetadata taskMeta = taskMetaOpt.get();

            for (GretlDslCall call : task.calls()) {
                Optional<PropertyMetadata> propMetaOpt = taskMeta.findProperty(call.name());
                if (propMetaOpt.isEmpty() || propMetaOpt.get().file() == null) {
                    continue;
                }

                List<Range> pathRanges = new ArrayList<>();
                List<String> paths = FileReferenceUtil.extractFilePathsWithRanges(call, pathRanges);
                for (int i = 0; i < paths.size(); i++) {
                    String relativePath = paths.get(i);
                    Path resolved = workspaceRoot.resolve(relativePath).normalize();
                    String targetUri = resolved.toUri().toString();
                    Range linkRange = i < pathRanges.size() ? pathRanges.get(i) : call.fullRange();

                    DocumentLink link = new DocumentLink();
                    link.setRange(linkRange);
                    link.setTarget(targetUri);
                    result.add(link);
                }
            }
        }
        return result;
    }
}
