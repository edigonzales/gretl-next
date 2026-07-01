package ch.so.agi.gretl.lsp.document;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class DocumentStore {

    private final Map<String, TextDocument> documents = new ConcurrentHashMap<>();

    public void open(String uri, String languageId, int version, String text) {
        documents.put(uri, new TextDocument(uri, languageId, version, text, LineIndex.from(text)));
    }

    public void changeFull(String uri, String text, int version) {
        documents.computeIfPresent(uri, (k, v) ->
                new TextDocument(uri, v.languageId(), version, text, LineIndex.from(text)));
    }

    public Optional<TextDocument> get(String uri) {
        return Optional.ofNullable(documents.get(uri));
    }

    public void close(String uri) {
        documents.remove(uri);
    }

    public Collection<TextDocument> allOpenDocuments() {
        return documents.values();
    }
}
