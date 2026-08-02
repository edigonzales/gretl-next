package ch.so.agi.gretl.test.fixture;

import ch.so.agi.gretl.test.job.TestJobExecutionIdentity;
import ch.so.agi.gretl.test.job.TestJobExecutionTarget;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class RecordingHttpTestFixture implements TestFixture {
    public static final String IMAGE = "python:3.12-alpine@sha256:6d43704baacd1bfbe7c295d7f13079d5d8104ed33568873133f8fc69980419df";
    private static final String SERVER_RESOURCE = "fixture-http-server.py";
    private static final String SERVER = """
            from http.server import BaseHTTPRequestHandler, HTTPServer
            import json, os
            logs = {}
            class Handler(BaseHTTPRequestHandler):
                def log_message(self, *args): pass
                def _body(self):
                    length = int(self.headers.get('Content-Length', '0'))
                    return self.rfile.read(length)
                def _send(self, code, body, content_type='text/plain'):
                    self.send_response(code); self.send_header('Content-Type', content_type)
                    self.send_header('Content-Length', str(len(body))); self.end_headers(); self.wfile.write(body)
                def do_GET(self):
                    if self.path == '/health': return self._send(200, b'ok')
                    if self.path == '/download': return self._send(200, b'download-content')
                    if self.path.startswith('/requests/'):
                        token = self.path.split('/', 2)[2]
                        return self._send(200, json.dumps(logs.get(token, [])).encode(), 'application/json')
                    return self._send(404, b'not found')
                def do_POST(self):
                    parts = self.path.split('/')
                    if len(parts) == 3 and parts[1] == 'reset': logs.pop(parts[2], None); return self._send(204, b'')
                    token = self.headers.get('X-GRETL-RUN-TOKEN', '')
                    body = self._body()
                    logs.setdefault(token, []).append({'method': 'POST', 'path': self.path, 'body': body.decode('utf-8', 'replace')})
                    return self._send(201 if self.path == '/text' else 200, b'accepted' if self.path == '/text' else (b'form-ok' if self.path == '/form' else b'ok'))
            HTTPServer(('0.0.0.0', 8080), Handler).serve_forever()
            """;

    private final GenericContainer<?> container;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private TestFixtureNetwork network;
    private boolean closed;

    public RecordingHttpTestFixture() {
        container = new GenericContainer<>(IMAGE)
                .withNetworkAliases("http")
                .withCopyFileToContainer(MountableFile.forHostPath(writeServer()), "/server.py")
                .withExposedPorts(8080)
                .withCommand("python", "/server.py")
                .waitingFor(Wait.forHttp("/health").forPort(8080).withStartupTimeout(Duration.ofSeconds(60)));
    }

    @Override public TestFixtureType type() { return TestFixtureType.HTTP; }

    @Override public synchronized void start(TestFixtureNetwork network) {
        if (closed) throw new IllegalStateException("HTTP fixture is closed");
        if (this.network != null) return;
        this.network = network;
        container.withNetwork(network.testcontainersNetwork()).start();
    }

    @Override public synchronized boolean isRunning() { return container.isRunning(); }

    @Override public synchronized TestFixtureLease acquire(TestJobExecutionIdentity identity) {
        if (!isRunning()) throw new IllegalStateException("HTTP fixture is not running");
        String token = identity.shortToken();
        return new HttpTestFixtureLease(this, token, identity.namespace(), "http-user", "http-password-" + token);
    }

    TestFixtureEndpointView endpoint(String token, String username, String password,
                                     TestJobExecutionTarget target) {
        String base = target == TestJobExecutionTarget.RUNTIME_IMAGE_ONE_SHOT
                || target == TestJobExecutionTarget.RUNTIME_IMAGE_SERVICE
                ? "http://http:8080" : "http://127.0.0.1:" + container.getMappedPort(8080);
        return new TestFixtureEndpointView(Map.of(
                "baseUrl", TestFixtureValue.publicValue(base),
                "username", TestFixtureValue.publicValue(username),
                "password", TestFixtureValue.secretValue(password),
                "runToken", TestFixtureValue.publicValue(token)),
                (target == TestJobExecutionTarget.RUNTIME_IMAGE_ONE_SHOT || target == TestJobExecutionTarget.RUNTIME_IMAGE_SERVICE)
                        ? Optional.of(network.dockerNetworkId()) : Optional.empty());
    }

    void reset(String token) {
        String base = "http://127.0.0.1:" + container.getMappedPort(8080);
        try {
            client.send(HttpRequest.newBuilder(URI.create(base + "/reset/" + token)).POST(HttpRequest.BodyPublishers.noBody()).build(),
                    HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            throw new IllegalStateException("Could not clean HTTP fixture lease", e);
        }
    }

    List<HttpRecordedRequest> requests(String token) {
        String base = "http://127.0.0.1:" + container.getMappedPort(8080);
        try {
            HttpResponse<String> response = client.send(
                    HttpRequest.newBuilder(URI.create(base + "/requests/" + token)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            var node = objectMapper.readTree(response.body());
            List<HttpRecordedRequest> result = new ArrayList<>();
            for (var request : node) {
                result.add(new HttpRecordedRequest(request.path("method").asText(),
                        request.path("path").asText(), request.path("body").asText()));
            }
            return List.copyOf(result);
        } catch (Exception e) {
            throw new IllegalStateException("Could not read HTTP fixture requests", e);
        }
    }

    @Override public synchronized void close() {
        if (closed) return;
        closed = true;
        if (container.isRunning()) container.stop();
    }

    private static String writeServer() {
        try {
            java.nio.file.Path file = java.nio.file.Files.createTempFile("gretl-http-fixture-", ".py");
            java.nio.file.Files.writeString(file, SERVER);
            file.toFile().deleteOnExit();
            return file.toString();
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Cannot create HTTP fixture server", e);
        }
    }
}
