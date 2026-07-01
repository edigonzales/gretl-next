package ch.so.agi.gretl.lsp.completion;

import ch.so.agi.gretl.lsp.model.GretlDependency;
import ch.so.agi.gretl.lsp.model.GretlScript;
import ch.so.agi.gretl.lsp.model.GretlTaskBlock;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

public final class CompletionContextDetector {

    public CompletionContext detect(GretlScript script, Position position, String currentLineText) {
        for (GretlTaskBlock block : script.tasks()) {
            if (block.typeRange() != null && positionInside(position, block.typeRange())) {
                return CompletionContext.of(CompletionContextKind.TASK_TYPE);
            }
            if (positionBetweenTaskNameAndBody(block, position)) {
                return CompletionContext.of(CompletionContextKind.TASK_TYPE);
            }
            if (block.bodyRange() != null && positionInside(position, block.bodyRange())) {
                for (GretlDependency dep : block.dependencies()) {
                    if (dep.range() != null && positionInside(position, dep.range())) {
                        return CompletionContext.of(CompletionContextKind.DEPENDENCY_TASK_NAME, block);
                    }
                }
                return CompletionContext.of(CompletionContextKind.INSIDE_GRETL_TASK_BODY, block);
            }
        }

        if (isTaskTypePosition(currentLineText, position)) {
            return CompletionContext.of(CompletionContextKind.TASK_TYPE);
        }

        return CompletionContext.of(CompletionContextKind.UNKNOWN);
    }

    static boolean positionInside(Position pos, Range range) {
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

    private boolean positionBetweenTaskNameAndBody(GretlTaskBlock block, Position position) {
        if (block.typeName().isPresent()) {
            return false;
        }
        if (block.nameRange() == null || block.bodyRange() == null) {
            return false;
        }
        if (position.getLine() < block.nameRange().getEnd().getLine()) {
            return false;
        }
        if (position.getLine() > block.bodyRange().getStart().getLine()) {
            return false;
        }
        if (position.getLine() == block.bodyRange().getStart().getLine()
                && position.getCharacter() >= block.bodyRange().getStart().getCharacter()) {
            return false;
        }
        return true;
    }

    private boolean isTaskTypePosition(String currentLineText, Position position) {
        if (currentLineText == null) {
            return false;
        }
        int col = Math.min(position.getCharacter(), currentLineText.length());
        String beforeCursor = currentLineText.substring(0, col);
        return beforeCursor.matches(".*tasks\\.register\\(\\s*['\"].*['\"]\\s*,\\s*.*");
    }
}
