package ch.so.agi.gretl.internal.http;

import java.nio.file.Path;
import java.util.Map;

public record CurlRequest(
        String serverUrl,
        CurlMethod method,
        Integer expectedStatusCode,
        String expectedBody,
        Map<String, String> headers,
        Map<String, Object> formData,
        String data,
        Path dataBinary,
        Path outputFile,
        String user,
        String password
) {
}
