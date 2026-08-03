package ch.so.agi.gretl;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurlFunctionalTest extends CoreFunctionalTestSupport {

    @Test
    void supportsGetPostBinaryMultipartAndOutputFile() throws Exception {
        writeSettings();
        Files.createDirectories(projectDir.resolve("data"));
        Files.writeString(projectDir.resolve("data/payload.txt"), "binary-content", StandardCharsets.UTF_8);

        AtomicReference<String> textBody = new AtomicReference<>();
        AtomicReference<String> binaryBody = new AtomicReference<>();
        AtomicReference<String> multipartBody = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();

        try (TestHttpServer server = TestHttpServer.start(exchange -> {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/text")) {
                textBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
                respond(exchange, 201, "accepted");
            } else if (path.equals("/binary")) {
                binaryBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                respond(exchange, 200, "ok");
            } else if (path.equals("/form")) {
                multipartBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                respond(exchange, 200, "form-ok");
            } else if (path.equals("/download")) {
                respond(exchange, 200, "download-content");
            } else {
                respond(exchange, 404, "missing");
            }
        })) {
            String endpoint = server.endpoint();
            writeBuild("""
                    plugins { id 'ch.so.agi.gretl' }

                    import ch.so.agi.gretl.tasks.Curl

                    tasks.register('postText', Curl) {
                        serverUrl '%s/text'
                        method Curl.MethodType.POST
                        expectedStatusCode 201
                        expectedBody 'accepted'
                        user 'reader'
                        password 'secret'
                        headers([X_Test: 'true'])
                        data 'hello-text'
                    }

                    tasks.register('postBinary', Curl) {
                        serverUrl '%s/binary'
                        method Curl.MethodType.POST
                        expectedStatusCode 200
                        expectedBody 'ok'
                        dataBinary 'data/payload.txt'
                    }

                    tasks.register('postForm', Curl) {
                        serverUrl '%s/form'
                        method Curl.MethodType.POST
                        expectedStatusCode 200
                        expectedBody 'form-ok'
                        formData([name: 'demo', payload: file('data/payload.txt')])
                    }

                    tasks.register('download', Curl) {
                        serverUrl '%s/download'
                        expectedStatusCode 200
                        outputFile layout.buildDirectory.file('out/download.txt')
                    }
                    """.formatted(endpoint, endpoint, endpoint, endpoint));

            run("postText", "postBinary", "postForm", "download");
        }

        assertEquals("hello-text", textBody.get());
        assertEquals("binary-content", binaryBody.get());
        assertTrue(multipartBody.get().contains("name=\"name\""));
        assertTrue(multipartBody.get().contains("demo"));
        assertTrue(multipartBody.get().contains("name=\"payload\""));
        assertTrue(multipartBody.get().contains("binary-content"));
        assertEquals("Basic " + Base64.getEncoder().encodeToString("reader:secret".getBytes(StandardCharsets.UTF_8)),
                authorization.get());
        assertEquals("download-content",
                Files.readString(projectDir.resolve("build/out/download.txt"), StandardCharsets.UTF_8));
    }

    @Test
    void preservesExistingOutputWhenResponseValidationFails() throws Exception {
        writeSettings();
        Path output = projectDir.resolve("build/out/download.txt");
        Files.createDirectories(output.getParent());
        Files.writeString(output, "previous", StandardCharsets.UTF_8);

        try (TestHttpServer server = TestHttpServer.start(exchange -> respond(exchange, 503, "partial"))) {
            writeBuild("""
                    plugins { id 'ch.so.agi.gretl' }

                    import ch.so.agi.gretl.tasks.Curl

                    tasks.register('download', Curl) {
                        serverUrl '%s/download'
                        expectedStatusCode 200
                        outputFile layout.buildDirectory.file('out/download.txt')
                    }
                    """.formatted(server.endpoint()));

            var result = runAndFail("download");
            assertTrue(result.getOutput().contains("Wrong status code returned: 503"), result.getOutput());
        }

        assertEquals("previous", Files.readString(output, StandardCharsets.UTF_8));
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private record TestHttpServer(HttpServer server) implements AutoCloseable {
        static TestHttpServer start(Handler handler) throws IOException {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", handler::handle);
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
            return new TestHttpServer(server);
        }

        String endpoint() {
            return "http://localhost:" + server.getAddress().getPort();
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }

    private interface Handler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
