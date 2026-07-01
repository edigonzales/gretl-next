package ch.so.agi.gretl.lsp.scanner;

import ch.so.agi.gretl.lsp.model.GretlParseProblem;
import ch.so.agi.gretl.lsp.model.GretlScript;
import ch.so.agi.gretl.lsp.parser.GretlScriptParser;
import ch.so.agi.gretl.lsp.parser.GroovyAstGretlParser;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import java.util.ArrayList;
import java.util.List;

public final class HybridGretlScriptParser implements GretlScriptParser {

    private final GroovyAstGretlParser astParser;
    private final LenientGretlScanner scanner;

    public HybridGretlScriptParser() {
        this.astParser = new GroovyAstGretlParser();
        this.scanner = new LenientGretlScanner();
    }

    @Override
    public GretlScript parse(String uri, String text) {
        GretlScript astResult;
        try {
            astResult = astParser.parse(uri, text);
        } catch (Exception e) {
            GretlScript scannerResult = scanner.parse(uri, text);
            List<GretlParseProblem> problems = new ArrayList<>(scannerResult.parseProblems());
            problems.add(new GretlParseProblem(
                    "AST parser failed, using scanner: " + e.getMessage(),
                    new Range(new Position(0, 0), new Position(0, 0)),
                    false));
            return mergeWithProblems(scannerResult, problems);
        }

        if (!astResult.tasks().isEmpty()) {
            if (astResult.parseProblems().isEmpty()
                    || astResult.tasks().size() >= countScannerTasks(uri, text)) {
                return astResult;
            }
        }

        GretlScript scannerResult = scanner.parse(uri, text);

        if (astResult.tasks().isEmpty() && !scannerResult.tasks().isEmpty()) {
            List<GretlParseProblem> combined = new ArrayList<>(scannerResult.parseProblems());
            combined.addAll(astResult.parseProblems());
            return mergeWithProblems(scannerResult, combined);
        }

        if (!astResult.tasks().isEmpty()) {
            return astResult;
        }

        return scannerResult;
    }

    private int countScannerTasks(String uri, String text) {
        try {
            return scanner.parse(uri, text).tasks().size();
        } catch (Exception e) {
            return 0;
        }
    }

    private GretlScript mergeWithProblems(GretlScript script, List<GretlParseProblem> additional) {
        List<GretlParseProblem> all = new ArrayList<>(script.parseProblems());
        all.addAll(additional);
        return new GretlScript(script.uri(), script.tasks(), script.defaultTasks(),
                script.variables(), all, script.astBased(), script.scannerFallbackUsed());
    }
}
