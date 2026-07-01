package ch.so.agi.gretl.lsp.document;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DocumentStoreTest {

    @Test
    @DisplayName("open stores document and allows retrieval")
    void openStoresDocument() {
        DocumentStore store = new DocumentStore();
        store.open("file:///test.gradle", "groovy", 1, "tasks.register('x', SqlExecutor) { }");

        var doc = store.get("file:///test.gradle");
        assertTrue(doc.isPresent());
        assertEquals("file:///test.gradle", doc.get().uri());
        assertEquals("groovy", doc.get().languageId());
        assertEquals(1, doc.get().version());
        assertEquals("tasks.register('x', SqlExecutor) { }", doc.get().text());
    }

    @Test
    @DisplayName("get returns empty if not open")
    void getReturnsEmpty() {
        DocumentStore store = new DocumentStore();
        assertTrue(store.get("nonexistent").isEmpty());
    }

    @Test
    @DisplayName("changeFull updates text and version")
    void changeFullUpdatesDocument() {
        DocumentStore store = new DocumentStore();
        store.open("file:///test.gradle", "groovy", 1, "old");
        store.changeFull("file:///test.gradle", "new", 2);

        var doc = store.get("file:///test.gradle");
        assertTrue(doc.isPresent());
        assertEquals("new", doc.get().text());
        assertEquals(2, doc.get().version());
    }

    @Test
    @DisplayName("close removes document")
    void closeRemovesDocument() {
        DocumentStore store = new DocumentStore();
        store.open("file:///test.gradle", "groovy", 1, "text");
        store.close("file:///test.gradle");

        assertTrue(store.get("file:///test.gradle").isEmpty());
    }

    @Test
    @DisplayName("close of unknown URI is a no-op")
    void closeUnknownUri() {
        DocumentStore store = new DocumentStore();
        assertDoesNotThrow(() -> store.close("nonexistent"));
    }

    @Test
    @DisplayName("allOpenDocuments returns all documents")
    void allOpenDocuments() {
        DocumentStore store = new DocumentStore();
        store.open("uri1", "groovy", 1, "a");
        store.open("uri2", "groovy", 1, "b");

        assertEquals(2, store.allOpenDocuments().size());
    }
}
