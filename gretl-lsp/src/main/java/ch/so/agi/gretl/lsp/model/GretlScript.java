package ch.so.agi.gretl.lsp.model;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public record GretlScript(String uri, List<GretlTaskBlock> tasks, List<DefaultTaskDeclaration> defaultTasks,
                           List<GretlVariableDeclaration> variables, List<GretlParseProblem> parseProblems,
                           boolean astBased, boolean scannerFallbackUsed) {

    public GretlScript {
        tasks = tasks != null ? List.copyOf(tasks) : List.of();
        defaultTasks = defaultTasks != null ? List.copyOf(defaultTasks) : List.of();
        variables = variables != null ? List.copyOf(variables) : List.of();
        parseProblems = parseProblems != null ? List.copyOf(parseProblems) : List.of();
    }

    public Optional<GretlTaskBlock> taskAt(Position position) {
        return tasks.stream()
                .filter(t -> positionInside(position, t.fullRange()))
                .findFirst();
    }

    public Optional<GretlTaskBlock> taskByName(String name) {
        return tasks.stream()
                .filter(t -> t.name().equals(name))
                .findFirst();
    }

    public Set<String> taskNames() {
        return tasks.stream()
                .map(GretlTaskBlock::name)
                .collect(Collectors.toSet());
    }

    private static boolean positionInside(Position pos, Range range) {
        if (pos.getLine() < range.getStart().getLine()) return false;
        if (pos.getLine() > range.getEnd().getLine()) return false;
        if (pos.getLine() == range.getStart().getLine()
                && pos.getCharacter() < range.getStart().getCharacter()) return false;
        if (pos.getLine() == range.getEnd().getLine()
                && pos.getCharacter() > range.getEnd().getCharacter()) return false;
        return true;
    }
}
