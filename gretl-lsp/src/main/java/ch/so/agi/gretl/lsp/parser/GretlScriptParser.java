package ch.so.agi.gretl.lsp.parser;

import ch.so.agi.gretl.lsp.model.GretlScript;

public interface GretlScriptParser {
    GretlScript parse(String uri, String text);
}
