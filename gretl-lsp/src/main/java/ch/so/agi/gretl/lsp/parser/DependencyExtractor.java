package ch.so.agi.gretl.lsp.parser;

import ch.so.agi.gretl.lsp.model.DependencyKind;
import ch.so.agi.gretl.lsp.model.GretlDependency;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.eclipse.lsp4j.Range;

import java.util.ArrayList;
import java.util.List;

import static ch.so.agi.gretl.lsp.parser.RangeConverter.toRange;

final class DependencyExtractor {

    private DependencyExtractor() {
    }

    static List<GretlDependency> extract(ClosureExpression closure, ExtractionContext ctx) {
        List<GretlDependency> deps = new ArrayList<>();
        Statement code = closure.getCode();
        if (code instanceof BlockStatement bs) {
            for (Statement stmt : bs.getStatements()) {
                deps.addAll(extractFromStatement(stmt, ctx));
            }
        } else {
            deps.addAll(extractFromStatement(code, ctx));
        }
        return deps;
    }

    static List<GretlDependency> extractTopLevel(Statement stmt, ExtractionContext ctx) {
        return extractFromStatement(stmt, ctx);
    }

    private static List<GretlDependency> extractFromStatement(Statement stmt, ExtractionContext ctx) {
        List<GretlDependency> deps = new ArrayList<>();
        if (!(stmt instanceof ExpressionStatement es)) {
            return deps;
        }
        org.codehaus.groovy.ast.expr.Expression expr = es.getExpression();
        if (!(expr instanceof MethodCallExpression mce)) {
            return deps;
        }
        String methodName = mce.getMethodAsString();
        if (methodName == null) {
            return deps;
        }
        DependencyKind kind = kindFromMethodName(methodName);
        if (kind == null) {
            return deps;
        }
        org.codehaus.groovy.ast.expr.Expression args = mce.getArguments();
        if (args instanceof ArgumentListExpression ale) {
            for (org.codehaus.groovy.ast.expr.Expression arg : ale.getExpressions()) {
                if (arg instanceof ConstantExpression ce && ce.getValue() instanceof String) {
                    String taskName = (String) ce.getValue();
                    Range range = toRange(arg, ctx);
                    deps.add(new GretlDependency(kind, taskName, range));
                }
            }
        }
        return deps;
    }

    private static DependencyKind kindFromMethodName(String methodName) {
        switch (methodName) {
            case "dependsOn":
                return DependencyKind.DEPENDS_ON;
            case "finalizedBy":
                return DependencyKind.FINALIZED_BY;
            case "mustRunAfter":
                return DependencyKind.MUST_RUN_AFTER;
            case "shouldRunAfter":
                return DependencyKind.SHOULD_RUN_AFTER;
            default:
                return null;
        }
    }
}
