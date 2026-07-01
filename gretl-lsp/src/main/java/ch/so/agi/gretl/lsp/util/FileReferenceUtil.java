package ch.so.agi.gretl.lsp.util;

import ch.so.agi.gretl.lsp.model.GretlArgument;
import ch.so.agi.gretl.lsp.model.GretlDslCall;
import ch.so.agi.gretl.lsp.model.GretlExpression;
import ch.so.agi.gretl.lsp.model.ListExpression;
import ch.so.agi.gretl.lsp.model.MapEntryExpression;
import ch.so.agi.gretl.lsp.model.MapExpression;
import ch.so.agi.gretl.lsp.model.MethodCallExpression;
import ch.so.agi.gretl.lsp.model.StringLiteralExpression;
import org.eclipse.lsp4j.Range;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FileReferenceUtil {

    private FileReferenceUtil() {
    }

    public static List<String> extractFilePaths(GretlDslCall call) {
        if (call == null || call.arguments().isEmpty()) {
            return List.of();
        }
        List<String> paths = new ArrayList<>();
        for (GretlArgument arg : call.arguments()) {
            paths.addAll(extractPaths(arg.expression()));
        }
        return Collections.unmodifiableList(paths);
    }

    public static List<String> extractFilePathsWithRanges(GretlDslCall call, List<Range> ranges) {
        if (call == null || call.arguments().isEmpty()) {
            return List.of();
        }
        List<String> paths = new ArrayList<>();
        for (GretlArgument arg : call.arguments()) {
            extractPathsWithRanges(arg.expression(), paths, ranges);
        }
        return Collections.unmodifiableList(paths);
    }

    private static List<String> extractPaths(GretlExpression expr) {
        List<String> paths = new ArrayList<>();
        extractPathsWithRanges(expr, paths, new ArrayList<>());
        return paths;
    }

    private static void extractPathsWithRanges(GretlExpression expr, List<String> paths, List<Range> ranges) {
        if (expr instanceof StringLiteralExpression stringLit) {
            paths.add(stringLit.value());
            ranges.add(stringLit.range());
        } else if (expr instanceof MethodCallExpression methodCall) {
            for (GretlArgument arg : methodCall.arguments()) {
                extractPathsWithRanges(arg.expression(), paths, ranges);
            }
        } else if (expr instanceof ListExpression listExpr) {
            for (GretlExpression element : listExpr.values()) {
                extractPathsWithRanges(element, paths, ranges);
            }
        }
    }

    public static List<String> extractMapKeys(GretlDslCall call) {
        if (call == null || call.arguments().isEmpty()) {
            return List.of();
        }
        List<String> keys = new ArrayList<>();
        for (GretlArgument arg : call.arguments()) {
            extractKeys(arg.expression(), keys);
        }
        return Collections.unmodifiableList(keys);
    }

    private static void extractKeys(GretlExpression expr, List<String> keys) {
        if (expr instanceof MapExpression mapExpr) {
            for (MapEntryExpression entry : mapExpr.entries()) {
                keys.add(entry.key());
            }
        } else if (expr instanceof ListExpression listExpr) {
            for (GretlExpression element : listExpr.values()) {
                extractKeys(element, keys);
            }
        }
    }
}
