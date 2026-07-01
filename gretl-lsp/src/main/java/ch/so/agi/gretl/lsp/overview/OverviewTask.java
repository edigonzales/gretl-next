package ch.so.agi.gretl.lsp.overview;

import java.util.List;

public record OverviewTask(String name, String typeName, int line, boolean allRequiredPresent,
                            List<String> requiredProperties) {

    public OverviewTask {
        requiredProperties = requiredProperties != null
                ? List.copyOf(requiredProperties) : List.of();
    }
}
