package ch.so.agi.gretl.lsp.scanner;

import ch.so.agi.gretl.lsp.document.LineIndex;
import ch.so.agi.gretl.lsp.model.*;
import ch.so.agi.gretl.lsp.parser.GretlScriptParser;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LenientGretlScanner implements GretlScriptParser {

    private static final Pattern TASKS_REGISTER = Pattern.compile(
            "tasks\\.register\\s*\\(\\s*['\"]([^'\"]+)['\"]\\s*,\\s*(\\w+(?:\\.\\w+)*)");
    private static final Pattern TASK_METHOD = Pattern.compile(
            "task\\s+['\"]?([\\w-]+)['\"]?\\s*\\(\\s*type\\s*:\\s*(\\w+(?:\\.\\w+)*)");

    private static final Pattern DSL_METHOD_CALL = Pattern.compile(
            "^\\s*(\\w+)\\s+(.+)$");
    private static final Pattern DEPENDENCY = Pattern.compile(
            "(dependsOn|finalizedBy|mustRunAfter|shouldRunAfter)\\s+['\"]([^'\"]+)['\"]");
    private static final Pattern DEFAULT_TASKS = Pattern.compile(
            "defaultTasks\\s+['\"]([^'\"]+)['\"]");

    @Override
    public GretlScript parse(String uri, String text) {
        LineIndex lineIndex = LineIndex.from(text);
        List<GretlTaskBlock> tasks = new ArrayList<>();
        List<DefaultTaskDeclaration> defaultTasks = new ArrayList<>();
        List<GretlParseProblem> problems = new ArrayList<>();

        defaultTasks.addAll(scanDefaultTasks(text, lineIndex));
        tasks.addAll(scanTaskHeaders(text, lineIndex));

        return new GretlScript(uri, tasks, defaultTasks, List.of(), problems, false, true);
    }

    private List<GretlTaskBlock> scanTaskHeaders(String text, LineIndex lineIndex) {
        List<GretlTaskBlock> tasks = new ArrayList<>();

        Matcher m = TASKS_REGISTER.matcher(text);
        while (m.find()) {
            String name = m.group(1);
            String typeName = m.group(2);
            int matchStart = m.start();
            int matchEnd = m.end();

            Position nameStart = lineIndex.positionAt(m.start(1));
            Position nameEnd = lineIndex.positionAt(m.end(1));

            Range nameRange = new Range(nameStart, nameEnd);
            Range typeRange = new Range(
                    lineIndex.positionAt(m.start(2)),
                    lineIndex.positionAt(m.end(2)));

            Position blockStart = lineIndex.positionAt(matchStart);
            Position blockEnd = lineIndex.positionAt(text.length());
            Range fullRange = new Range(blockStart, blockEnd);

            Range bodyRange = findClosureRange(matchEnd, text, lineIndex);

            List<GretlDslCall> calls = new ArrayList<>();
            List<GretlDependency> deps = new ArrayList<>();

            if (bodyRange != null) {
                deps.addAll(scanDependenciesInside(bodyRange, text, lineIndex));
                calls.addAll(scanCallsInside(bodyRange, text, lineIndex));
            }

            fullRange = new Range(blockStart,
                    bodyRange != null ? bodyRange.getEnd() : blockEnd);

            tasks.add(new GretlTaskBlock(name, Optional.ofNullable(typeName),
                    nameRange, typeRange, fullRange, bodyRange != null ? bodyRange : fullRange,
                    calls, deps, List.of()));
        }

        if (tasks.isEmpty()) {
            m = TASK_METHOD.matcher(text);
            while (m.find()) {
                String name = m.group(1);
                String typeName = m.group(2);

                Range nameRange = new Range(
                        lineIndex.positionAt(m.start(1)), lineIndex.positionAt(m.end(1)));
                Range typeRange = new Range(
                        lineIndex.positionAt(m.start(2)), lineIndex.positionAt(m.end(2)));

                Range bodyRange = findClosureRange(m.end(), text, lineIndex);
                Range fullRange = new Range(lineIndex.positionAt(m.start()),
                        bodyRange != null ? bodyRange.getEnd() : lineIndex.positionAt(text.length()));

                List<GretlDslCall> calls = new ArrayList<>();
                List<GretlDependency> deps = new ArrayList<>();

                if (bodyRange != null) {
                    deps.addAll(scanDependenciesInside(bodyRange, text, lineIndex));
                    calls.addAll(scanCallsInside(bodyRange, text, lineIndex));
                }

                tasks.add(new GretlTaskBlock(name, Optional.ofNullable(typeName),
                        nameRange, typeRange, fullRange, bodyRange != null ? bodyRange : fullRange,
                        calls, deps, List.of()));
            }
        }

        return tasks;
    }

    private Range findClosureRange(int startOffset, String text, LineIndex lineIndex) {
        int braceStart = text.indexOf('{', startOffset);
        if (braceStart < 0) {
            return null;
        }

        int depth = 0;
        for (int i = braceStart; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return new Range(lineIndex.positionAt(braceStart),
                            lineIndex.positionAt(i + 1));
                }
            }
        }
        return null;
    }

    private List<GretlDslCall> scanCallsInside(Range bodyRange, String text, LineIndex lineIndex) {
        List<GretlDslCall> calls = new ArrayList<>();
        int bodyStart = lineIndex.offsetAt(bodyRange.getStart());
        int bodyEnd = lineIndex.offsetAt(bodyRange.getEnd());

        if (bodyEnd <= bodyStart + 1) {
            return calls;
        }

        String body = text.substring(bodyStart + 1, bodyEnd - 1);
        String[] lines = body.split("\\r?\\n");

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("//")) {
                continue;
            }

            Matcher dslMatch = DSL_METHOD_CALL.matcher(rawLine);
            if (dslMatch.matches()) {
                String name = dslMatch.group(1);
                if (name.equals("dependsOn") || name.equals("finalizedBy")
                        || name.equals("mustRunAfter") || name.equals("shouldRunAfter")
                        || name.equals("doLast") || name.equals("doFirst") || name.equals("onlyIf")) {
                    continue;
                }
                String argsText = dslMatch.group(2).trim();
                int nameStartInBody = body.indexOf(name);
                if (nameStartInBody < 0) continue;
                int nameStart = bodyStart + 1 + nameStartInBody;
                int nameEnd = nameStart + name.length();
                int argsEnd = nameStart + rawLine.trim().length();

                Range nameRange = new Range(lineIndex.positionAt(nameStart),
                        lineIndex.positionAt(nameEnd));
                Range fullRange = new Range(lineIndex.positionAt(nameStart),
                        lineIndex.positionAt(Math.min(argsEnd, text.length())));

                List<GretlArgument> arguments = new ArrayList<>();
                arguments.add(new GretlArgument(
                        new StringLiteralExpression(argsText, fullRange, argsText),
                        fullRange, Optional.empty()));

                calls.add(new GretlDslCall(name, DslCallStyle.METHOD_CALL,
                        nameRange, fullRange, arguments, name + " " + argsText));
            }
        }
        return calls;
    }

    private List<GretlDependency> scanDependenciesInside(Range bodyRange, String text, LineIndex lineIndex) {
        List<GretlDependency> deps = new ArrayList<>();
        int bodyStart = lineIndex.offsetAt(bodyRange.getStart());
        int bodyEnd = lineIndex.offsetAt(bodyRange.getEnd());
        String body = text.substring(bodyStart, bodyEnd);

        Matcher m = DEPENDENCY.matcher(body);
        while (m.find()) {
            String kind = m.group(1);
            String taskName = m.group(2);

            DependencyKind depKind;
            switch (kind) {
                case "dependsOn":
                    depKind = DependencyKind.DEPENDS_ON;
                    break;
                case "finalizedBy":
                    depKind = DependencyKind.FINALIZED_BY;
                    break;
                case "mustRunAfter":
                    depKind = DependencyKind.MUST_RUN_AFTER;
                    break;
                case "shouldRunAfter":
                    depKind = DependencyKind.SHOULD_RUN_AFTER;
                    break;
                default:
                    continue;
            }

            int absStart = bodyStart + m.start(2);
            int absEnd = bodyStart + m.end(2);
            Range range = new Range(lineIndex.positionAt(absStart),
                    lineIndex.positionAt(absEnd));

            deps.add(new GretlDependency(depKind, taskName, range));
        }
        return deps;
    }

    private List<DefaultTaskDeclaration> scanDefaultTasks(String text, LineIndex lineIndex) {
        List<DefaultTaskDeclaration> tasks = new ArrayList<>();
        Matcher m = DEFAULT_TASKS.matcher(text);
        while (m.find()) {
            String taskName = m.group(1);
            Range range = new Range(lineIndex.positionAt(m.start(1)),
                    lineIndex.positionAt(m.end(1)));
            tasks.add(new DefaultTaskDeclaration(taskName, range));
        }
        return tasks;
    }
}
