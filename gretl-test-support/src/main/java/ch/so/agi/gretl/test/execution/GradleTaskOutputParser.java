package ch.so.agi.gretl.test.execution;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GradleTaskOutputParser {
    private static final Pattern TASK = Pattern.compile("^>(?: Task)? (:[^ ]+) (SUCCESS|FAILED|SKIPPED|UP-TO-DATE|FROM-CACHE|NO-SOURCE).*$",
            Pattern.MULTILINE);

    public Map<String, GretlTaskOutcome> parse(String output) {
        Map<String, GretlTaskOutcome> outcomes = new LinkedHashMap<>();
        Matcher matcher = TASK.matcher(output == null ? "" : output);
        while (matcher.find()) {
            outcomes.put(matcher.group(1), switch (matcher.group(2)) {
                case "SUCCESS" -> GretlTaskOutcome.SUCCESS;
                case "FAILED" -> GretlTaskOutcome.FAILED;
                case "SKIPPED" -> GretlTaskOutcome.SKIPPED;
                case "UP-TO-DATE" -> GretlTaskOutcome.UP_TO_DATE;
                case "FROM-CACHE" -> GretlTaskOutcome.FROM_CACHE;
                case "NO-SOURCE" -> GretlTaskOutcome.NO_SOURCE;
                default -> GretlTaskOutcome.UNKNOWN;
            });
        }
        return Map.copyOf(outcomes);
    }
}
