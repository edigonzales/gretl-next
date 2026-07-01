package ch.so.agi.gretl.lsp.parser;

import ch.so.agi.gretl.lsp.document.LineIndex;
import ch.so.agi.gretl.lsp.model.*;
import org.eclipse.lsp4j.Range;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static ch.so.agi.gretl.lsp.parser.RangeConverter.emptyRange;
import static ch.so.agi.gretl.lsp.parser.RangeConverter.toRange;

public final class ExpressionConverter {

    private ExpressionConverter() {
    }

    static GretlExpression convert(org.codehaus.groovy.ast.expr.Expression expr, ExtractionContext ctx) {
        if (expr == null) {
            return new UnknownExpression(emptyRange(), "");
        }

        Range range = toRange(expr, ctx);

        if (expr instanceof org.codehaus.groovy.ast.expr.ConstantExpression ce) {
            Object value = ce.getValue();
            String text = ce.getText();
            if (value instanceof String || value instanceof org.codehaus.groovy.runtime.GStringImpl) {
                return new StringLiteralExpression(text, range, text);
            }
            if (value instanceof Boolean) {
                return new BooleanLiteralExpression((Boolean) value, range, text);
            }
            if (value instanceof Number) {
                return new NumberLiteralExpression(text, range, text);
            }
            return new StringLiteralExpression(text, range, text);
        }

        if (expr instanceof org.codehaus.groovy.ast.expr.VariableExpression ve) {
            String name = ve.getName();
            return new VariableExpression(name, range, name);
        }

        if (expr instanceof org.codehaus.groovy.ast.expr.MethodCallExpression mce) {
            String methodName = extractMethodName(mce);
            List<GretlArgument> arguments = extractArguments(mce.getArguments(), ctx);
            String sourceText = safeSource(range, ctx);
            return new MethodCallExpression(methodName, arguments, range, sourceText);
        }

        if (expr instanceof org.codehaus.groovy.ast.expr.ListExpression le) {
            List<GretlExpression> values = new ArrayList<>();
            for (org.codehaus.groovy.ast.expr.Expression e : le.getExpressions()) {
                values.add(convert(e, ctx));
            }
            String sourceText = safeSource(range, ctx);
            return new ListExpression(values, range, sourceText);
        }

        if (expr instanceof org.codehaus.groovy.ast.expr.MapExpression me) {
            List<MapEntryExpression> entries = new ArrayList<>();
            for (org.codehaus.groovy.ast.expr.MapEntryExpression gee : me.getMapEntryExpressions()) {
                GretlExpression key = convert(gee.getKeyExpression(), ctx);
                GretlExpression value = convert(gee.getValueExpression(), ctx);
                String keyName = key instanceof StringLiteralExpression s ? s.value() : key.sourceText();
                entries.add(new MapEntryExpression(keyName, value, toRange(gee, ctx)));
            }
            String sourceText = safeSource(range, ctx);
            return new MapExpression(entries, range, sourceText);
        }

        if (expr instanceof org.codehaus.groovy.ast.expr.NamedArgumentListExpression nale) {
            List<MapEntryExpression> entries = new ArrayList<>();
            for (org.codehaus.groovy.ast.expr.MapEntryExpression gee : nale.getMapEntryExpressions()) {
                GretlExpression key = convert(gee.getKeyExpression(), ctx);
                GretlExpression value = convert(gee.getValueExpression(), ctx);
                String keyName = key instanceof StringLiteralExpression s ? s.value() : key.sourceText();
                entries.add(new MapEntryExpression(keyName, value, toRange(gee, ctx)));
            }
            String sourceText = safeSource(range, ctx);
            return new MapExpression(entries, range, sourceText);
        }

        if (expr instanceof org.codehaus.groovy.ast.expr.GStringExpression gse) {
            String text = gse.getText();
            return new StringLiteralExpression(text, range, text);
        }

        if (expr instanceof org.codehaus.groovy.ast.expr.PropertyExpression
                || expr instanceof org.codehaus.groovy.ast.expr.BinaryExpression
                || expr instanceof org.codehaus.groovy.ast.expr.DeclarationExpression
                || expr instanceof org.codehaus.groovy.ast.expr.ClosureExpression
                || expr instanceof org.codehaus.groovy.ast.expr.ClassExpression
                || expr instanceof org.codehaus.groovy.ast.expr.TupleExpression) {
            String text = getTextSafely(expr, range, ctx);
            return new UnknownExpression(range, text);
        }

        String text = getTextSafely(expr, range, ctx);
        return new UnknownExpression(range, text);
    }

    private static String extractMethodName(org.codehaus.groovy.ast.expr.MethodCallExpression mce) {
        org.codehaus.groovy.ast.expr.Expression method = mce.getMethod();
        if (method instanceof org.codehaus.groovy.ast.expr.ConstantExpression ce) {
            Object value = ce.getValue();
            if (value instanceof String) {
                return (String) value;
            }
        }
        if (method != null) {
            String text = method.getText();
            if (text != null) {
                return text;
            }
        }
        return "unknown";
    }

    static List<GretlArgument> extractArguments(org.codehaus.groovy.ast.expr.Expression argsExpr, ExtractionContext ctx) {
        List<GretlArgument> arguments = new ArrayList<>();
        if (argsExpr instanceof org.codehaus.groovy.ast.expr.ArgumentListExpression ale) {
            for (org.codehaus.groovy.ast.expr.Expression e : ale.getExpressions()) {
                if (e instanceof org.codehaus.groovy.ast.expr.NamedArgumentListExpression nale) {
                    for (org.codehaus.groovy.ast.expr.MapEntryExpression gee : nale.getMapEntryExpressions()) {
                        GretlExpression value = convert(gee.getValueExpression(), ctx);
                        GretlExpression keyExpr = convert(gee.getKeyExpression(), ctx);
                        String keyName = keyExpr instanceof StringLiteralExpression s ? s.value() : keyExpr.sourceText();
                        arguments.add(new GretlArgument(value, toRange(gee, ctx), Optional.of(keyName)));
                    }
                } else if (e instanceof org.codehaus.groovy.ast.expr.MapExpression me) {
                    for (org.codehaus.groovy.ast.expr.MapEntryExpression gee : me.getMapEntryExpressions()) {
                        GretlExpression value = convert(gee.getValueExpression(), ctx);
                        GretlExpression keyExpr = convert(gee.getKeyExpression(), ctx);
                        String keyName = keyExpr instanceof StringLiteralExpression s ? s.value() : keyExpr.sourceText();
                        arguments.add(new GretlArgument(value, toRange(gee, ctx), Optional.of(keyName)));
                    }
                } else if (e instanceof org.codehaus.groovy.ast.expr.MapEntryExpression gee) {
                    GretlExpression key = convert(gee.getKeyExpression(), ctx);
                    GretlExpression value = convert(gee.getValueExpression(), ctx);
                    String keyName = key instanceof StringLiteralExpression s ? s.value() : key.sourceText();
                    arguments.add(new GretlArgument(value, toRange(gee, ctx), Optional.of(keyName)));
                } else {
                    GretlExpression converted = convert(e, ctx);
                    arguments.add(new GretlArgument(converted, toRange(e, ctx), Optional.empty()));
                }
            }
        } else if (argsExpr != null) {
            GretlExpression converted = convert(argsExpr, ctx);
            arguments.add(new GretlArgument(converted, toRange(argsExpr, ctx), Optional.empty()));
        }
        return arguments;
    }

    private static String getTextSafely(org.codehaus.groovy.ast.expr.Expression expr, Range range, ExtractionContext ctx) {
        try {
            int start = ctx.lineIndex().offsetAt(range.getStart());
            int end = ctx.lineIndex().offsetAt(range.getEnd());
            if (start >= 0 && end <= ctx.text().length() && start <= end) {
                return ctx.text().substring(start, end);
            }
        } catch (Exception e) {
            // fall through
        }
        String text = expr.getText();
        return text != null ? text : "unknown";
    }

    private static String safeSource(Range range, ExtractionContext ctx) {
        try {
            int start = ctx.lineIndex().offsetAt(range.getStart());
            int end = ctx.lineIndex().offsetAt(range.getEnd());
            if (start >= 0 && end <= ctx.text().length() && start <= end) {
                return ctx.text().substring(start, end);
            }
        } catch (Exception e) {
            // fall through
        }
        return "";
    }
}
