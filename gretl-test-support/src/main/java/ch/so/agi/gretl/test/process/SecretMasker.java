package ch.so.agi.gretl.test.process;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Central secret masking facade for process output, commands and environments. */
public final class SecretMasker {
    private final SecretRedactor delegate = new SecretRedactor();

    public String mask(String text, Set<String> secrets) {
        return delegate.redact(text, secrets);
    }

    public List<String> maskArguments(List<String> arguments, Set<String> secrets) {
        return delegate.redact(arguments, secrets);
    }

    public Map<String, String> maskEnvironment(Map<String, String> environment, Set<String> secrets) {
        return environment.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                Map.Entry::getKey, entry -> delegate.redact(entry.getValue(), secrets)));
    }
}
