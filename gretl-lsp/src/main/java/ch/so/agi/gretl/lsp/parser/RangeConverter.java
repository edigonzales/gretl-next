package ch.so.agi.gretl.lsp.parser;

import ch.so.agi.gretl.lsp.document.LineIndex;
import org.codehaus.groovy.ast.ASTNode;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

public final class RangeConverter {

    private RangeConverter() {
    }

    static Range toRange(ASTNode node, ExtractionContext ctx) {
        int startLine = node.getLineNumber() - 1;
        int startCol = node.getColumnNumber() - 1;
        int endLine = node.getLastLineNumber() - 1;
        int endCol = node.getLastColumnNumber() - 1;

        if (startLine < 0) startLine = 0;
        if (endLine < 0) endLine = 0;
        if (startCol < 0) startCol = 0;

        if (endCol < 0) {
            String lineText = ctx.lineIndex().lineText(endLine);
            endCol = lineText != null ? lineText.length() : 0;
        }

        return new Range(new Position(startLine, startCol), new Position(endLine, endCol));
    }

    static Range emptyRange() {
        return new Range(new Position(0, 0), new Position(0, 0));
    }
}
