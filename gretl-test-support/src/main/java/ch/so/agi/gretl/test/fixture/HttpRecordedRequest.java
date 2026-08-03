package ch.so.agi.gretl.test.fixture;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public record HttpRecordedRequest(String method, String path, String contentType, long bodyLength, String bodySha256,
                                  Optional<String> textBody, boolean authenticated, Map<String, String> safeHeaders,
                                  Set<String> multipartFieldNames, Map<String, String> multipartFileSha256) {
    public HttpRecordedRequest {
        if (method == null || method.isBlank() || path == null || path.isBlank()) throw new IllegalArgumentException("HTTP method and path are required");
        contentType = contentType == null ? "" : contentType;
        if (bodyLength < 0 || bodySha256 == null || bodySha256.isBlank()) throw new IllegalArgumentException("HTTP body metadata is invalid");
        textBody = textBody == null ? Optional.empty() : textBody;
        safeHeaders = Map.copyOf(safeHeaders == null ? Map.of() : safeHeaders);
        multipartFieldNames = Set.copyOf(multipartFieldNames == null ? Set.of() : multipartFieldNames);
        multipartFileSha256 = Map.copyOf(multipartFileSha256 == null ? Map.of() : multipartFileSha256);
        if (safeHeaders.keySet().stream().anyMatch(key -> key.equalsIgnoreCase("Authorization"))) throw new IllegalArgumentException("Authorization must not be recorded");
    }
}
