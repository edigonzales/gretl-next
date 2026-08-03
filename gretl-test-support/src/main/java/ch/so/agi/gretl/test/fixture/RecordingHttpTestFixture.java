package ch.so.agi.gretl.test.fixture;

import ch.so.agi.gretl.test.job.TestJobExecutionIdentity;
import ch.so.agi.gretl.test.job.TestJobExecutionTarget;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RecordingHttpTestFixture implements TestFixture {
    public static final String IMAGE = "python:3.12-alpine@sha256:6d43704baacd1bfbe7c295d7f13079d5d8104ed33568873133f8fc69980419df";
    private final GenericContainer<?> container = new GenericContainer<>(IMAGE)
            .withNetworkAliases("http")
            .withCopyFileToContainer(MountableFile.forClasspathResource("fixture-http-server.py"), "/server.py")
            .withExposedPorts(8080)
            .withCommand("python", "/server.py")
            .waitingFor(Wait.forHttp("/health").forPort(8080).withStartupTimeout(Duration.ofSeconds(60)));
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private TestFixtureNetwork network;
    private boolean closed;

    @Override public TestFixtureType type() { return TestFixtureType.HTTP; }

    @Override public synchronized void start(TestFixtureStartContext context) {
        if (closed) throw new IllegalStateException("HTTP fixture is closed");
        if (network != null) return;
        network = context.requireNetwork(type());
        container.withNetwork(network.testcontainersNetwork()).start();
    }

    @Override public synchronized boolean isRunning() { return container.isRunning(); }

    @Override public synchronized TestFixtureLease acquire(TestJobExecutionIdentity identity) {
        if (!isRunning()) throw new IllegalStateException("HTTP fixture is not running");
        String token = identity.shortToken();
        String username = "http-user";
        String password = "http-password-" + token;
        configure(token, username, password);
        return new HttpTestFixtureLease(this, token, identity.namespace(), username, password);
    }

    TestFixtureEndpointView endpoint(String token, String username, String password, TestJobExecutionTarget target) {
        boolean runtime = target == TestJobExecutionTarget.RUNTIME_IMAGE_ONE_SHOT || target == TestJobExecutionTarget.RUNTIME_IMAGE_SERVICE;
        return new TestFixtureEndpointView(Map.of("baseUrl", TestFixtureValue.publicValue(runtime ? "http://http:8080" : hostBase()),
                "username", TestFixtureValue.publicValue(username), "password", TestFixtureValue.secretValue(password),
                "runToken", TestFixtureValue.publicValue(token)), runtime ? Optional.of(network.dockerNetworkId()) : Optional.empty());
    }

    void configure(String token, String username, String password) {
        try {
            HttpResponse<Void> response = client.send(HttpRequest.newBuilder(URI.create(hostBase() + "/configure/" + token))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(Map.of("username", username, "password", password))))
                    .build(), HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() != 204) throw new IllegalStateException("HTTP fixture configuration failed");
        } catch (Exception e) { throw new IllegalStateException("Could not configure HTTP fixture lease", e); }
    }

    void reset(String token) { sendControl("/reset/" + token, HttpRequest.BodyPublishers.noBody()); }

    void deleteConfiguration(String token) {
        try { client.send(HttpRequest.newBuilder(URI.create(hostBase() + "/configure/" + token)).DELETE().build(), HttpResponse.BodyHandlers.discarding()); }
        catch (Exception e) { throw new IllegalStateException("Could not delete HTTP fixture lease configuration", e); }
    }

    List<HttpRecordedRequest> requests(String token) {
        try {
            HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create(hostBase() + "/requests/" + token)).GET().build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw new IllegalStateException("HTTP fixture request log failed: " + response.statusCode());
            var node = objectMapper.readTree(response.body());
            List<HttpRecordedRequest> result = new ArrayList<>();
            for (var request : node) {
                Map<String, String> headers = new LinkedHashMap<>();
                request.path("safeHeaders").fields().forEachRemaining(entry -> headers.put(entry.getKey(), entry.getValue().asText()));
                java.util.Set<String> fields = new LinkedHashSet<>(); request.path("multipartFieldNames").forEach(value -> fields.add(value.asText()));
                Map<String, String> files = new LinkedHashMap<>(); request.path("multipartFileSha256").fields().forEachRemaining(entry -> files.put(entry.getKey(), entry.getValue().asText()));
                result.add(new HttpRecordedRequest(request.path("method").asText(), request.path("path").asText(), request.path("contentType").asText(),
                        request.path("bodyLength").asLong(), request.path("bodySha256").asText(), request.path("textBody").isNull() ? Optional.empty() : Optional.of(request.path("textBody").asText()),
                        request.path("authenticated").asBoolean(), headers, fields, files));
            }
            return List.copyOf(result);
        } catch (Exception e) { throw new IllegalStateException("Could not read HTTP fixture requests", e); }
    }

    @Override public synchronized void close() {
        if (closed) return;
        closed = true;
        if (container.isRunning()) container.stop();
    }

    private void sendControl(String path, HttpRequest.BodyPublisher body) {
        try {
            HttpResponse<Void> response = client.send(HttpRequest.newBuilder(URI.create(hostBase() + path)).POST(body).build(), HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() != 204) throw new IllegalStateException("HTTP fixture control request failed: " + response.statusCode());
        }
        catch (Exception e) { throw new IllegalStateException("Could not clean HTTP fixture lease", e); }
    }

    private String hostBase() { return "http://127.0.0.1:" + container.getMappedPort(8080); }
}
