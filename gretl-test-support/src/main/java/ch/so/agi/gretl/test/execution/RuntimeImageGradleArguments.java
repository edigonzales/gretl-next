package ch.so.agi.gretl.test.execution;

import java.util.ArrayList;
import java.util.List;

public final class RuntimeImageGradleArguments {
    public List<String> arguments(RuntimeExecutionMode executionMode, List<String> requested) {
        if (executionMode == null) {
            throw new IllegalArgumentException("executionMode must not be null");
        }
        if (requested == null) {
            throw new IllegalArgumentException("requested arguments must not be null");
        }
        if (requested.contains("--refresh-dependencies")) {
            throw new IllegalArgumentException(
                    "The GRETL runtime image uses bundled-only dependency resolution.\n"
                            + "Argument '--refresh-dependencies' is not supported.");
        }
        List<String> arguments = new ArrayList<>(requested);
        addIfAbsent(arguments, "--offline");
        addIfAbsent(arguments, "--console=plain");
        switch (executionMode) {
            case ONE_SHOT -> {
                if (arguments.contains("--daemon")) {
                    throw new IllegalArgumentException("ONE_SHOT must not use --daemon");
                }
                addIfAbsent(arguments, "--no-daemon");
            }
            case SERVICE -> {
                if (arguments.contains("--no-daemon")) {
                    throw new IllegalArgumentException("SERVICE must not use --no-daemon");
                }
                addIfAbsent(arguments, "--daemon");
            }
        }
        return List.copyOf(arguments);
    }

    private void addIfAbsent(List<String> arguments, String value) {
        if (!arguments.contains(value)) {
            arguments.add(value);
        }
    }
}
