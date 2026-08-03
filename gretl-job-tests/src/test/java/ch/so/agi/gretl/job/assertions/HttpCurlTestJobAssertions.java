package ch.so.agi.gretl.job.assertions;

import ch.so.agi.gretl.test.fixture.HttpRecordedRequest;
import ch.so.agi.gretl.test.fixture.HttpTestFixtureLease;
import ch.so.agi.gretl.test.job.TestJobVerificationContext;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class HttpCurlTestJobAssertions extends AbstractCanonicalTestJobAssertions {
    @Override public String id() { return "network-http-curl"; }
    @Override protected void verifyJob(TestJobVerificationContext context) throws Exception {
        HttpTestFixtureLease lease = context.requireFixture("http", HttpTestFixtureLease.class);
        List<HttpRecordedRequest> requests = lease.requests();
        assertEquals(List.of("/text", "/binary", "/form", "/download"), requests.stream().map(HttpRecordedRequest::path).toList());
        HttpRecordedRequest text = requests.get(0), binary = requests.get(1), form = requests.get(2);
        assertTrue(text.authenticated()); assertEquals("hello-text", text.textBody().orElseThrow());
        assertEquals(sha256("hello-text".getBytes(StandardCharsets.UTF_8)), text.bodySha256());
        byte[] payload = Files.readAllBytes(context.job().resolve("input/payload.bin"));
        assertTrue(binary.authenticated()); assertEquals(payload.length, binary.bodyLength()); assertEquals(sha256(payload), binary.bodySha256());
        assertTrue(form.authenticated(), form.toString());
        assertTrue(form.multipartFieldNames().contains("payload"), form.toString());
        assertEquals(sha256(payload), form.multipartFileSha256().get("payload"), form.toString());
        assertEquals(Files.readString(context.job().resolveExpected("download.txt")), Files.readString(context.job().resolve("build/download/payload.bin")));
        assertTrue(requests.stream().allMatch(HttpRecordedRequest::authenticated));
        assertTrue(requests.stream().noneMatch(request -> request.safeHeaders().keySet().stream().anyMatch(key -> key.equalsIgnoreCase("Authorization"))));
    }

    private static String sha256(byte[] value) throws Exception {
        return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
    }
}
