package ch.so.agi.gretl.test.execution;

import java.util.ArrayList;
import java.util.List;

public final class RuntimeImageGradleArguments {
    public List<String> arguments(RuntimeInvocationProfile profile, List<String> requested) {
        List<String> arguments = new ArrayList<>(requested);
        if (arguments.stream().noneMatch(value -> value.equals("--console=plain"))) {
            arguments.add("--console=plain");
        }
        switch (profile) {
            case ONE_SHOT_ONLINE -> addIfAbsent(arguments, "--no-daemon");
            case ONE_SHOT_OFFLINE -> {
                addIfAbsent(arguments, "--no-daemon");
                addIfAbsent(arguments, "--offline");
            }
            case LONG_LIVED_DAEMON -> {
                if (arguments.stream().anyMatch(value -> value.equals("--no-daemon"))) {
                    throw new IllegalArgumentException("LONG_LIVED_DAEMON must not use --no-daemon");
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
