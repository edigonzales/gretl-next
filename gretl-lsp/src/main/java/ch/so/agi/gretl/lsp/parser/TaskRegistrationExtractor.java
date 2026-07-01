package ch.so.agi.gretl.lsp.parser;

import ch.so.agi.gretl.lsp.model.GretlTaskBlock;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.MapEntryExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.NamedArgumentListExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static ch.so.agi.gretl.lsp.parser.RangeConverter.toRange;

final class TaskRegistrationExtractor {

    private TaskRegistrationExtractor() {
    }

    static Optional<GretlTaskBlock> fromMethodCall(MethodCallExpression call, ExtractionContext ctx) {
        if (isTasksRegister(call)) {
            return extractTasksRegister(call, ctx);
        }
        if (isTaskMethodCall(call)) {
            return extractTaskMethodCall(call, ctx);
        }
        return Optional.empty();
    }

    private static boolean isTasksRegister(MethodCallExpression call) {
        org.codehaus.groovy.ast.expr.Expression obj = call.getObjectExpression();
        String objText = null;
        if (obj instanceof PropertyExpression pe) {
            objText = pe.getPropertyAsString();
            if (objText == null) {
                objText = pe.getText();
            }
        } else if (obj instanceof VariableExpression ve) {
            objText = ve.getName();
        }
        return "tasks".equals(objText) && "register".equals(call.getMethodAsString());
    }

    private static Optional<GretlTaskBlock> extractTasksRegister(MethodCallExpression call, ExtractionContext ctx) {
        org.codehaus.groovy.ast.expr.Expression args = call.getArguments();
        if (!(args instanceof ArgumentListExpression ale)) {
            return Optional.empty();
        }
        List<org.codehaus.groovy.ast.expr.Expression> argList = ale.getExpressions();
        if (argList.size() < 2) {
            return Optional.empty();
        }

        String taskName = extractStringValue(argList.get(0));
        if (taskName == null) {
            return Optional.empty();
        }

        String typeName = extractTypeName(argList.get(1));
        Range nameRange = toRange(argList.get(0), ctx);
        Range typeRange = toRange(argList.get(1), ctx);
        Range fullRange = toRange(call, ctx);

        ClosureExpression closure = findClosure(argList);
        Range bodyRange;
        if (closure != null) {
            bodyRange = toRange(closure, ctx);
        } else {
            int startOff = ctx.lineIndex().offsetAt(fullRange.getStart());
            int endOff = ctx.lineIndex().offsetAt(fullRange.getEnd());
            bodyRange = new Range(ctx.lineIndex().positionAt(startOff),
                    ctx.lineIndex().positionAt(endOff));
        }

        return Optional.of(new GretlTaskBlock(taskName, Optional.ofNullable(typeName),
                nameRange, typeRange, fullRange, bodyRange, List.of(), List.of(), List.of()));
    }

    private static boolean isTaskMethodCall(MethodCallExpression call) {
        org.codehaus.groovy.ast.expr.Expression obj = call.getObjectExpression();
        String method = call.getMethodAsString();
        if ("task".equals(method) && obj == null) {
            return true;
        }
        if ("task".equals(method) && obj instanceof VariableExpression ve && "this".equals(ve.getName())) {
            return true;
        }
        return false;
    }

    private static Optional<GretlTaskBlock> extractTaskMethodCall(MethodCallExpression call, ExtractionContext ctx) {
        org.codehaus.groovy.ast.expr.Expression args = call.getArguments();
        if (!(args instanceof ArgumentListExpression ale)) {
            return Optional.empty();
        }
        List<org.codehaus.groovy.ast.expr.Expression> argList = ale.getExpressions();
        if (argList.isEmpty()) {
            return Optional.empty();
        }

        String taskName = null;
        String typeName = null;
        Range nameRange = toRange(call, ctx);
        Range typeRange = toRange(call, ctx);
        ClosureExpression closure = findClosure(argList);

        for (org.codehaus.groovy.ast.expr.Expression arg : argList) {
            if (arg instanceof MapEntryExpression gee) {
                String key = extractStringValue(gee.getKeyExpression());
                if ("type".equals(key)) {
                    typeName = extractTypeName(gee.getValueExpression());
                    typeRange = toRange(gee.getValueExpression(), ctx);
                }
            } else if (arg instanceof ConstantExpression ce && (ce.getValue() instanceof String)) {
                taskName = (String) ce.getValue();
                nameRange = toRange(arg, ctx);
            } else if (!(arg instanceof NamedArgumentListExpression) && !(arg instanceof ClosureExpression)) {
                if (taskName == null && arg instanceof VariableExpression) {
                    taskName = ((VariableExpression) arg).getName();
                    nameRange = toRange(arg, ctx);
                }
            }
        }

        if (taskName == null && !argList.isEmpty()) {
            org.codehaus.groovy.ast.expr.Expression first = argList.get(0);
            String text = first.getText();
            if (text != null && !text.startsWith("type:")) {
                taskName = text;
                nameRange = toRange(first, ctx);
            }
        }

        if (taskName == null) {
            return Optional.empty();
        }

        Range fullRange = toRange(call, ctx);
        Range bodyRange;
        if (closure != null) {
            bodyRange = toRange(closure, ctx);
        } else {
            int startOff = ctx.lineIndex().offsetAt(fullRange.getStart());
            int endOff = ctx.lineIndex().offsetAt(fullRange.getEnd());
            bodyRange = new Range(ctx.lineIndex().positionAt(startOff),
                    ctx.lineIndex().positionAt(endOff));
        }

        return Optional.of(new GretlTaskBlock(taskName, Optional.ofNullable(typeName),
                nameRange, typeRange, fullRange, bodyRange, List.of(), List.of(), List.of()));
    }

    static ClosureExpression findClosure(List<org.codehaus.groovy.ast.expr.Expression> args) {
        for (org.codehaus.groovy.ast.expr.Expression arg : args) {
            if (arg instanceof ClosureExpression) {
                return (ClosureExpression) arg;
            }
        }
        return null;
    }

    private static String extractStringValue(org.codehaus.groovy.ast.expr.Expression expr) {
        if (expr instanceof ConstantExpression ce) {
            Object value = ce.getValue();
            if (value instanceof String) {
                return (String) value;
            }
        }
        return null;
    }

    private static String extractTypeName(org.codehaus.groovy.ast.expr.Expression expr) {
        if (expr instanceof ConstantExpression ce) {
            Object value = ce.getValue();
            if (value instanceof String) {
                return (String) value;
            }
        }
        if (expr instanceof ClassExpression ce) {
            String name = ce.getType().getNameWithoutPackage();
            if (name == null || name.isEmpty()) {
                name = ce.getText();
            }
            return name;
        }
        if (expr instanceof VariableExpression) {
            return expr.getText();
        }
        if (expr instanceof PropertyExpression pe) {
            return pe.getPropertyAsString();
        }
        return expr != null ? expr.getText() : null;
    }
}
