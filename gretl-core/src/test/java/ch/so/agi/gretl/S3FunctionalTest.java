package ch.so.agi.gretl;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class S3FunctionalTest extends CoreFunctionalTestSupport {

    @Test
    void uploadsCopiesDownloadsAndDeletesObjects() throws Exception {
        writeSettings();
        Files.createDirectories(projectDir.resolve("data/dir"));
        Files.writeString(projectDir.resolve("data/one.txt"), "one", StandardCharsets.UTF_8);
        Files.writeString(projectDir.resolve("data/two.txt"), "two", StandardCharsets.UTF_8);
        Files.writeString(projectDir.resolve("data/dir/three.txt"), "three", StandardCharsets.UTF_8);

        try (FakeS3Server s3 = FakeS3Server.start()) {
            writeBuild("""
                    plugins { id 'ch.so.agi.gretl' }

                    import ch.so.agi.gretl.tasks.S3Upload
                    import ch.so.agi.gretl.tasks.S3Bucket2Bucket
                    import ch.so.agi.gretl.tasks.S3Download
                    import ch.so.agi.gretl.tasks.S3Delete

                    def s3Endpoint = '%s'
                    def common = {
                        accessKey 'access'
                        secretKey 'secret'
                        endpoint s3Endpoint
                        region 'us-east-1'
                    }

                    tasks.register('uploadFile', S3Upload) {
                        common.delegate = delegate; common()
                        bucketName 'source'
                        sourceFile 'data/one.txt'
                        acl 'private'
                    }

                    tasks.register('uploadFiles', S3Upload) {
                        common.delegate = delegate; common()
                        bucketName 'source'
                        sourceFiles files('data/two.txt')
                        sourceDir 'data/dir'
                        acl 'private'
                        contentType 'text/plain'
                        metadata([source: 'test'])
                    }

                    tasks.register('copyBuckets', S3Bucket2Bucket) {
                        dependsOn 'uploadFile', 'uploadFiles'
                        accessKey 'access'
                        secretKey 'secret'
                        endpoint s3Endpoint
                        region 'us-east-1'
                        sourceBucket 'source'
                        targetBucket 'target'
                        acl 'private'
                        metadata([copied: 'true'])
                    }

                    tasks.register('downloadKey', S3Download) {
                        dependsOn 'copyBuckets'
                        common.delegate = delegate; common()
                        bucketName 'target'
                        key 'one.txt'
                        downloadDir layout.buildDirectory.dir('download-key').get().asFile
                    }

                    tasks.register('downloadAll', S3Download) {
                        dependsOn 'copyBuckets'
                        common.delegate = delegate; common()
                        bucketName 'target'
                        downloadDir layout.buildDirectory.dir('download-all').get().asFile
                    }

                    tasks.register('deleteKey', S3Delete) {
                        dependsOn 'downloadKey', 'downloadAll'
                        common.delegate = delegate; common()
                        bucketName 'target'
                        key 'one.txt'
                    }

                    tasks.register('deleteAll', S3Delete) {
                        dependsOn 'deleteKey'
                        common.delegate = delegate; common()
                        bucketName 'target'
                    }
                    """.formatted(s3.endpoint()));

            run("deleteAll");

            assertEquals("one", Files.readString(projectDir.resolve("build/download-key/one.txt"), StandardCharsets.UTF_8));
            assertEquals("one", Files.readString(projectDir.resolve("build/download-all/one.txt"), StandardCharsets.UTF_8));
            assertEquals("two", Files.readString(projectDir.resolve("build/download-all/two.txt"), StandardCharsets.UTF_8));
            assertEquals("three", Files.readString(projectDir.resolve("build/download-all/three.txt"), StandardCharsets.UTF_8));
            assertTrue(s3.bucket("source").containsKey("one.txt"));
            assertTrue(s3.bucket("source").containsKey("two.txt"));
            assertTrue(s3.bucket("source").containsKey("three.txt"));
            assertFalse(s3.bucket("target").containsKey("one.txt"));
            assertTrue(s3.bucket("target").isEmpty());
        }
    }

    private record FakeS3Server(HttpServer server, Map<String, Map<String, byte[]>> buckets) implements AutoCloseable {

        static FakeS3Server start() throws IOException {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            Map<String, Map<String, byte[]>> buckets = new ConcurrentHashMap<>();
            FakeS3Server fake = new FakeS3Server(server, buckets);
            server.createContext("/", fake::handle);
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
            return fake;
        }

        String endpoint() {
            return "http://localhost:" + server.getAddress().getPort();
        }

        Map<String, byte[]> bucket(String name) {
            return buckets.computeIfAbsent(name, ignored -> new ConcurrentHashMap<>());
        }

        @Override
        public void close() {
            server.stop(0);
        }

        private void handle(HttpExchange exchange) throws IOException {
            try {
                RequestPath path = RequestPath.parse(exchange.getRequestURI().getRawPath());
                String method = exchange.getRequestMethod();
                String query = exchange.getRequestURI().getRawQuery();
                if ("GET".equals(method) && query != null && query.contains("list-type=2")) {
                    respondXml(exchange, 200, listXml(path.bucket()));
                } else if ("GET".equals(method)) {
                    byte[] body = bucket(path.bucket()).get(path.key());
                    if (body == null) {
                        respond(exchange, 404, new byte[0]);
                    } else {
                        respond(exchange, 200, body);
                    }
                } else if ("PUT".equals(method) && exchange.getRequestHeaders().containsKey("x-amz-copy-source")) {
                    copyObject(exchange, path);
                } else if ("PUT".equals(method)) {
                    byte[] body = decodeAwsChunkedIfNeeded(exchange, exchange.getRequestBody().readAllBytes());
                    bucket(path.bucket()).put(path.key(), body);
                    exchange.getResponseHeaders().add("ETag", quote(md5(body)));
                    respond(exchange, 200, new byte[0]);
                } else if ("DELETE".equals(method)) {
                    bucket(path.bucket()).remove(path.key());
                    respond(exchange, 204, new byte[0]);
                } else {
                    respond(exchange, 405, new byte[0]);
                }
            } catch (Exception e) {
                respond(exchange, 500, e.getMessage().getBytes(StandardCharsets.UTF_8));
            }
        }

        private void copyObject(HttpExchange exchange, RequestPath target) throws IOException {
            String source = exchange.getRequestHeaders().getFirst("x-amz-copy-source");
            if (source.startsWith("/")) {
                source = source.substring(1);
            }
            String[] parts = decode(source).split("/", 2);
            byte[] body = bucket(parts[0]).get(parts[1]);
            if (body == null) {
                respond(exchange, 404, new byte[0]);
                return;
            }
            bucket(target.bucket()).put(target.key(), Arrays.copyOf(body, body.length));
            respondXml(exchange, 200, """
                    <CopyObjectResult xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
                      <LastModified>2026-01-01T00:00:00.000Z</LastModified>
                      <ETag>%s</ETag>
                    </CopyObjectResult>
                    """.formatted(quote(md5(body))));
        }

        private String listXml(String bucket) {
            String contents = bucket(bucket).entrySet().stream()
                    .map(entry -> """
                            <Contents>
                              <Key>%s</Key>
                              <LastModified>2026-01-01T00:00:00.000Z</LastModified>
                              <ETag>%s</ETag>
                              <Size>%d</Size>
                              <StorageClass>STANDARD</StorageClass>
                            </Contents>
                            """.formatted(xml(entry.getKey()), quote(md5(entry.getValue())), entry.getValue().length))
                    .collect(Collectors.joining());
            return """
                    <ListBucketResult xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
                      <Name>%s</Name>
                      <Prefix></Prefix>
                      <KeyCount>%d</KeyCount>
                      <MaxKeys>1000</MaxKeys>
                      <IsTruncated>false</IsTruncated>
                      %s
                    </ListBucketResult>
                    """.formatted(xml(bucket), bucket(bucket).size(), contents);
        }

        private static void respondXml(HttpExchange exchange, int status, String body) throws IOException {
            exchange.getResponseHeaders().add("Content-Type", "application/xml");
            respond(exchange, status, body.getBytes(StandardCharsets.UTF_8));
        }

        private static void respond(HttpExchange exchange, int status, byte[] body) throws IOException {
            exchange.sendResponseHeaders(status, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        }

        private static String decode(String value) {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        }

        private static String xml(String value) {
            return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        }

        private static byte[] decodeAwsChunkedIfNeeded(HttpExchange exchange, byte[] body) throws IOException {
            String encoding = exchange.getRequestHeaders().getFirst("Content-Encoding");
            String decodedLength = exchange.getRequestHeaders().getFirst("x-amz-decoded-content-length");
            if ((encoding == null || !encoding.contains("aws-chunked"))
                    && decodedLength == null
                    && !looksAwsChunked(body)) {
                return body;
            }

            ByteArrayOutputStream decoded = new ByteArrayOutputStream();
            int offset = 0;
            while (offset < body.length) {
                int lineEnd = crlf(body, offset);
                if (lineEnd < 0) {
                    throw new IOException("Invalid aws-chunked request body");
                }
                String chunkHeader = new String(body, offset, lineEnd - offset, StandardCharsets.US_ASCII);
                String chunkSize = chunkHeader.split(";", 2)[0].trim();
                int size = Integer.parseInt(chunkSize, 16);
                offset = lineEnd + 2;
                if (size == 0) {
                    return decoded.toByteArray();
                }
                if (offset + size > body.length) {
                    throw new IOException("Invalid aws-chunked chunk size");
                }
                decoded.write(body, offset, size);
                offset += size;
                if (offset + 1 < body.length && body[offset] == '\r' && body[offset + 1] == '\n') {
                    offset += 2;
                }
            }
            return decoded.toByteArray();
        }

        private static boolean looksAwsChunked(byte[] body) {
            int length = Math.min(body.length, 128);
            String prefix = new String(body, 0, length, StandardCharsets.US_ASCII);
            return prefix.contains("chunk-signature");
        }

        private static int crlf(byte[] body, int offset) {
            for (int i = offset; i < body.length - 1; i++) {
                if (body[i] == '\r' && body[i + 1] == '\n') {
                    return i;
                }
            }
            return -1;
        }

        private static String quote(String value) {
            return "\"" + value + "\"";
        }

        private static String md5(byte[] body) {
            try {
                return HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(body));
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private record RequestPath(String bucket, String key) {
        static RequestPath parse(String rawPath) {
            String path = rawPath.startsWith("/") ? rawPath.substring(1) : rawPath;
            String[] parts = path.split("/", 2);
            String bucket = decode(parts[0]);
            String key = parts.length > 1 ? decode(parts[1]) : "";
            return new RequestPath(bucket, key);
        }

        private static String decode(String value) {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        }
    }
}
