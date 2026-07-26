package ch.so.agi.gretl.doclet.internal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TechnicalTextRendererTest {
    private final TechnicalTextRenderer renderer = new TechnicalTextRenderer();

    @Test
    void rendersAcronymsAtTheTechnicalPrefixOfGermanCompounds() {
        assertEquals(
                "[.acronym]#SQL#-Dateien, [.acronym]#JDBC#-URL und [.acronym]#INTERLIS#-Modell",
                renderer.render("SQL-Dateien, JDBC-URL und INTERLIS-Modell"));
    }

    @Test
    void rendersBareAndCodeFormattedMethodReferences() {
        assertEquals(
                "Mit [.dsl-method]#`schema(\\...)`# und [.dsl-method]#`xslFile(\\...)`# konfigurieren.",
                renderer.render("Mit schema(...) und `xslFile(...)` konfigurieren."));
    }

    @Test
    void protectsExistingCodeLinksAndRoles() {
        String text = "Bereits [.acronym]#SQL#, [.acronym]##ID##s, `JDBC` "
                + "und https://example.test[SQL-Link].";
        assertEquals(text, renderer.render(text));
    }
}
