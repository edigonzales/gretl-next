package ch.so.agi.gretl.lsp.parser;

import ch.so.agi.gretl.lsp.model.*;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.eclipse.lsp4j.Range;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static ch.so.agi.gretl.lsp.parser.ExpressionConverter.extractArguments;
import static ch.so.agi.gretl.lsp.parser.RangeConverter.toRange;

final class DslCallExtractor {

    private static final Set<String> GRADLE_INTERNAL_METHODS = Set.of(
            "dependsOn", "finalizedBy", "mustRunAfter", "shouldRunAfter",
            "doLast", "doFirst", "onlyIf", "configure", "description", "group",
            "enabled", "inputs", "outputs", "ext"
    );

    private DslCallExtractor() {
    }

    static List<GretlDslCall> extractCalls(ClosureExpression closure, ExtractionContext ctx) {
        List<GretlDslCall> calls = new ArrayList<>();
        Statement code = closure.getCode();
        if (code instanceof BlockStatement bs) {
            for (Statement stmt : bs.getStatements()) {
                extractFromStatement(stmt, ctx).ifPresent(calls::add);
            }
        } else {
            extractFromStatement(code, ctx).ifPresent(calls::add);
        }
        return calls;
    }

    private static Optional<GretlDslCall> extractFromStatement(Statement stmt, ExtractionContext ctx) {
        if (!(stmt instanceof ExpressionStatement es)) {
            return Optional.empty();
        }
        org.codehaus.groovy.ast.expr.Expression expr = es.getExpression();
        return extractFromExpression(expr, ctx);
    }

    private static Optional<GretlDslCall> extractFromExpression(
            org.codehaus.groovy.ast.expr.Expression expr, ExtractionContext ctx) {

        if (expr instanceof BinaryExpression be
                && be.getOperation().getText().equals("=")
                && be.getLeftExpression() instanceof org.codehaus.groovy.ast.expr.VariableExpression ve) {
            return extractAssignment(ve, be.getRightExpression(), ctx);
        }

        if (expr instanceof org.codehaus.groovy.ast.expr.MethodCallExpression mce) {
            return extractMethodCall(mce, ctx);
        }

        return Optional.empty();
    }

    private static Optional<GretlDslCall> extractAssignment(
            org.codehaus.groovy.ast.expr.VariableExpression left,
            org.codehaus.groovy.ast.expr.Expression right,
            ExtractionContext ctx) {
        String name = left.getName();

        Range nameRange = toRange(left, ctx);
        List<GretlArgument> arguments = extractArguments(right, ctx);
        Range rightRange = toRange(right, ctx);

        int startOff = ctx.lineIndex().offsetAt(nameRange.getStart());
        int endOff = ctx.lineIndex().offsetAt(rightRange.getEnd());
        String sourceText = ctx.text().substring(startOff, Math.min(endOff, ctx.text().length()));

        Range fullRange = new Range(nameRange.getStart(), rightRange.getEnd());

        return Optional.of(new GretlDslCall(name, DslCallStyle.ASSIGNMENT, nameRange, fullRange,
                arguments, sourceText));
    }

    private static Optional<GretlDslCall> extractMethodCall(
            org.codehaus.groovy.ast.expr.MethodCallExpression mce, ExtractionContext ctx) {
        String methodName = mce.getMethodAsString();
        if (methodName == null) {
            org.codehaus.groovy.ast.expr.Expression methodExpr = mce.getMethod();
            if (methodExpr instanceof ConstantExpression ce && ce.getValue() instanceof String) {
                methodName = (String) ce.getValue();
            }
        }
        if (methodName == null) {
            return Optional.empty();
        }

        org.codehaus.groovy.ast.expr.Expression objExpr = mce.getObjectExpression();
        if (objExpr instanceof org.codehaus.groovy.ast.expr.VariableExpression
                && !"this".equals(((org.codehaus.groovy.ast.expr.VariableExpression) objExpr).getName())) {
            return Optional.empty();
        }

        DslCallStyle style = DslCallStyle.METHOD_CALL;
        if (objExpr instanceof org.codehaus.groovy.ast.expr.PropertyExpression pe) {
            String propName = pe.getPropertyAsString();
            if (methodName.equals("set") && propName != null) {
                style = DslCallStyle.SET_METHOD;
            }
        }

        List<GretlArgument> arguments = extractArguments(mce.getArguments(), ctx);
        Range nameRange = toRange(mce.getMethod(), ctx);
        Range fullRange = toRange(mce, ctx);

        int startOff = ctx.lineIndex().offsetAt(nameRange.getStart());
        int endOff = ctx.lineIndex().offsetAt(fullRange.getEnd());
        String sourceText = ctx.text().substring(startOff, Math.min(endOff, ctx.text().length()));

        return Optional.of(new GretlDslCall(methodName, style, nameRange, fullRange,
                arguments, sourceText));
    }

    static boolean isGradleInternal(String methodName) {
        return GRADLE_INTERNAL_METHODS.contains(methodName);
    }
}
