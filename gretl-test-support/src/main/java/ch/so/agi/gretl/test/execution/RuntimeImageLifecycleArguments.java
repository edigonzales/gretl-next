package ch.so.agi.gretl.test.execution;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds only arguments that describe the Gradle lifecycle used by the runtime
 * container. Dependency policy is owned by the {@code gretl} launcher.
 */
public final class RuntimeImageLifecycleArguments {
    public List<String> arguments(RuntimeExecutionMode executionMode, List<String> requestedArguments) {
        if (executionMode == null) {
            throw new IllegalArgumentException("executionMode must not be null");
        }
        if (requestedArguments == null) {
            throw new IllegalArgumentException("requested arguments must not be null");
        }
        if (requestedArguments.contains("--refresh-dependencies")) {
            throw new IllegalArgumentException(
                    "The GRETL runtime image allows local-only Gradle dependency resolution only; "
                            + "remote downloads are disabled. Argument '--refresh-dependencies' is not supported.");
        }

        List<String> arguments = new ArrayList<>(requestedArguments);
        addIfAbsent(arguments, "--console=plain");
        switch (executionMode) {
            case ONE_SHOT -> {
                if (arguments.contains("--daemon")) {
                    throw new IllegalArgumentException("ONE_SHOT execution must not use --daemon.");
                }
                addIfAbsent(arguments, "--no-daemon");
            }
            case SERVICE -> {
                if (arguments.contains("--no-daemon")) {
                    throw new IllegalArgumentException("SERVICE execution must not use --no-daemon.");
                }
                addIfAbsent(arguments, "--daemon");
            }
        }
        return List.copyOf(arguments);
    }

    private void addIfAbsent(List<String> arguments, String argument) {
        if (!arguments.contains(argument)) {
            arguments.add(argument);
        }
    }
}
