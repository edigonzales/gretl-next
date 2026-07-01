package ch.so.agi.gretl.lsp.parser;

import ch.so.agi.gretl.lsp.document.LineIndex;
import ch.so.agi.gretl.lsp.model.*;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.Phases;
import org.codehaus.groovy.control.SourceUnit;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static ch.so.agi.gretl.lsp.parser.RangeConverter.toRange;

public final class GroovyAstGretlParser implements GretlScriptParser {

    private static final CompilerConfiguration DEFAULT_CONFIG = createConfig();

    private static CompilerConfiguration createConfig() {
        CompilerConfiguration config = new CompilerConfiguration();
        config.setTargetBytecode(CompilerConfiguration.JDK8);
        return config;
    }

    @Override
    public GretlScript parse(String uri, String text) {
        LineIndex lineIndex = LineIndex.from(text);
        ExtractionContext ctx = new ExtractionContext(uri, text, lineIndex);

        try {
            ModuleNode module = parseGroovyModule(uri, text);
            return fromAst(module, ctx);
        } catch (Exception e) {
            GretlParseProblem problem = new GretlParseProblem(
                    "Groovy-AST-parsing failed: " + e.getMessage(),
                    new Range(new Position(0, 0), new Position(0, 0)),
                    true);
            return new GretlScript(uri, List.of(), List.of(), List.of(),
                    List.of(problem), false, false);
        }
    }

    private GretlScript fromAst(ModuleNode module, ExtractionContext ctx) {
        List<GretlTaskBlock> tasks = new ArrayList<>();
        List<DefaultTaskDeclaration> defaultTasks = new ArrayList<>();
        List<GretlDependency> topLevelDeps = new ArrayList<>();
        List<GretlParseProblem> problems = new ArrayList<>();

        Statement block = module.getStatementBlock();
        if (block instanceof BlockStatement bs) {
            for (Statement stmt : bs.getStatements()) {
                Optional<GretlTaskBlock> taskBlock = tryExtractTask(stmt, ctx);
                if (taskBlock.isPresent()) {
                    tasks.add(taskBlock.get());
                } else {
                    tryExtractDefaultTasks(stmt, ctx).ifPresent(defaultTasks::add);
                    topLevelDeps.addAll(DependencyExtractor.extractTopLevel(stmt, ctx));
                }
            }
        }

        return new GretlScript(ctx.uri(), tasks, defaultTasks, List.of(), problems,
                true, false);
    }

    private Optional<GretlTaskBlock> tryExtractTask(Statement stmt, ExtractionContext ctx) {
        if (!(stmt instanceof ExpressionStatement es)) {
            return Optional.empty();
        }
        org.codehaus.groovy.ast.expr.Expression expr = es.getExpression();
        if (!(expr instanceof MethodCallExpression mce)) {
            return Optional.empty();
        }

        Optional<GretlTaskBlock> base = TaskRegistrationExtractor.fromMethodCall(mce, ctx);
        if (base.isEmpty()) {
            return Optional.empty();
        }

        GretlTaskBlock taskBlock = base.get();
        List<GretlDslCall> calls = new ArrayList<>();
        List<GretlDependency> deps = new ArrayList<>();
        List<GretlExpression> rawExpressions = new ArrayList<>();

        ClosureExpression closure = findClosureInCall(mce);
        if (closure != null) {
            List<GretlDslCall> all = DslCallExtractor.extractCalls(closure, ctx);
            for (GretlDslCall call : all) {
                if (DslCallExtractor.isGradleInternal(call.name())) {
                    rawExpressions.add(new UnknownExpression(call.fullRange(), call.sourceText()));
                } else {
                    calls.add(call);
                }
            }
            deps.addAll(DependencyExtractor.extract(closure, ctx));
        }

        return Optional.of(new GretlTaskBlock(taskBlock.name(), taskBlock.typeName(),
                taskBlock.nameRange(), taskBlock.typeRange(), taskBlock.fullRange(),
                taskBlock.bodyRange(), calls, deps, rawExpressions));
    }

    private Optional<DefaultTaskDeclaration> tryExtractDefaultTasks(Statement stmt, ExtractionContext ctx) {
        if (!(stmt instanceof ExpressionStatement es)) {
            return Optional.empty();
        }
        org.codehaus.groovy.ast.expr.Expression expr = es.getExpression();
        if (!(expr instanceof MethodCallExpression mce)) {
            return Optional.empty();
        }
        String methodName = mce.getMethodAsString();
        if (!"defaultTasks".equals(methodName)) {
            return Optional.empty();
        }
        org.codehaus.groovy.ast.expr.Expression args = mce.getArguments();
        if (args instanceof ArgumentListExpression ale) {
            for (org.codehaus.groovy.ast.expr.Expression arg : ale.getExpressions()) {
                if (arg instanceof ConstantExpression ce && ce.getValue() instanceof String) {
                    String taskName = (String) ce.getValue();
                    Range range = toRange(arg, ctx);
                    return Optional.of(new DefaultTaskDeclaration(taskName, range));
                }
            }
        }
        return Optional.empty();
    }

    private static ClosureExpression findClosureInCall(MethodCallExpression mce) {
        org.codehaus.groovy.ast.expr.Expression args = mce.getArguments();
        return TaskRegistrationExtractor.findClosure(
                args instanceof ArgumentListExpression ale ? ale.getExpressions() : List.of());
    }

    private ModuleNode parseGroovyModule(String uri, String text) {
        CompilerConfiguration config = new CompilerConfiguration(DEFAULT_CONFIG);
        CompilationUnit cu = new CompilationUnit(config);
        try {
            SourceUnit source = cu.addSource(uri, text);
            cu.compile(Phases.CONVERSION);
            return source.getAST();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Groovy source: " + e.getMessage(), e);
        }
    }
}
