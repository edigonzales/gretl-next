package ch.so.agi.gretl.internal.http;

import ch.so.agi.gretl.internal.io.SafeFileOutput;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class CurlEngine {

    public void execute(CurlRequest request) throws IOException, InterruptedException {
        validate(request);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(request.serverUrl()))
                .timeout(Duration.ofMinutes(5));

        if (request.user() != null && request.password() != null) {
            String token = Base64.getEncoder().encodeToString(
                    (request.user() + ":" + request.password()).getBytes(StandardCharsets.UTF_8));
            builder.header("Authorization", "Basic " + token);
        }
        request.headers().forEach(builder::header);

        Body body = body(request);
        body.headers().forEach(builder::header);

        CurlMethod method = request.method();
        if (method == null) {
            method = body.empty()
                    ? CurlMethod.GET
                    : CurlMethod.POST;
        }
        if (method == CurlMethod.GET) {
            builder.GET();
        } else {
            builder.POST(body.publisher());
        }

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<byte[]> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() != request.expectedStatusCode()) {
            throw new IOException("Wrong status code returned: " + response.statusCode());
        }
        if (request.expectedBody() != null) {
            String bodyText = new String(response.body(), StandardCharsets.UTF_8);
            if (!bodyText.contains(request.expectedBody())) {
                throw new IOException("Response body does not contain expected string: " + bodyText);
            }
        }
        if (request.outputFile() != null) {
            SafeFileOutput.writeAtomically(request.outputFile(), output -> Files.write(output, response.body()));
        }
    }

    private static void validate(CurlRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        if (isBlank(request.serverUrl())) {
            throw new IllegalArgumentException("serverUrl must not be null");
        }
        if (request.expectedStatusCode() == null) {
            throw new IllegalArgumentException("expectedStatusCode must not be null");
        }
        if (request.data() != null && request.dataBinary() != null) {
            throw new IllegalArgumentException("Use either data or dataBinary, not both.");
        }
        if (request.formData() != null && (!request.formData().isEmpty())
                && (request.data() != null || request.dataBinary() != null)) {
            throw new IllegalArgumentException("Use either formData, data or dataBinary, not multiple request bodies.");
        }
    }

    private static Body body(CurlRequest request) throws IOException {
        if (request.formData() != null && !request.formData().isEmpty()) {
            return multipartBody(request.formData());
        }
        if (request.dataBinary() != null) {
            return new Body(HttpRequest.BodyPublishers.ofFile(request.dataBinary()), Map.of(), false);
        }
        if (request.data() != null) {
            return new Body(HttpRequest.BodyPublishers.ofString(request.data(), StandardCharsets.UTF_8),
                    Map.of("Content-Type", "text/plain; charset=UTF-8"), false);
        }
        return new Body(HttpRequest.BodyPublishers.noBody(), Map.of(), true);
    }

    private static Body multipartBody(Map<String, Object> formData) throws IOException {
        String boundary = "gretl-" + UUID.randomUUID();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<byte[]> chunks = new ArrayList<>();
        for (Map.Entry<String, Object> entry : formData.entrySet()) {
            Object value = entry.getValue();
            out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            if (value instanceof Path path) {
                writeFilePart(out, entry.getKey(), path);
            } else if (value instanceof java.io.File file) {
                writeFilePart(out, entry.getKey(), file.toPath());
            } else {
                out.write(("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"\r\n\r\n")
                        .getBytes(StandardCharsets.UTF_8));
                out.write(String.valueOf(value).getBytes(StandardCharsets.UTF_8));
                out.write("\r\n".getBytes(StandardCharsets.UTF_8));
            }
        }
        out.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        chunks.add(out.toByteArray());
        return new Body(HttpRequest.BodyPublishers.ofByteArrays(chunks),
                Map.of("Content-Type", "multipart/form-data; boundary=" + boundary), false);
    }

    private static void writeFilePart(ByteArrayOutputStream out, String name, Path path) throws IOException {
        out.write(("Content-Disposition: form-data; name=\"" + name + "\"; filename=\""
                + path.getFileName() + "\"\r\n").getBytes(StandardCharsets.UTF_8));
        out.write("Content-Type: application/octet-stream\r\n\r\n".getBytes(StandardCharsets.UTF_8));
        Files.copy(path, out);
        out.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record Body(HttpRequest.BodyPublisher publisher, Map<String, String> headers, boolean empty) {
    }
}
