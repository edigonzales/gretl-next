package ch.so.agi.gretl;

import io.floci.testcontainers.FlociContainer;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ch.so.agi.gretl.testkit.GretlBuildExecutors;
import ch.so.agi.gretl.testkit.GretlTestProjectSettings;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class S3FlociIntegrationTest {

    @Container
    static final FlociContainer FLOCI = new FlociContainer().withRegion("us-east-1");

    @TempDir
    Path projectDir;

    @Test
    void uploadsCopiesDownloadsAndDeletesObjectsWithFloci() throws Exception {
        String sourceBucket = bucket("source");
        String targetBucket = bucket("target");
        try (S3Client s3 = s3()) {
            s3.createBucket(builder -> builder.bucket(sourceBucket));
            s3.createBucket(builder -> builder.bucket(targetBucket));
        }

        writeSettings();
        Files.createDirectories(projectDir.resolve("data/dir"));
        Files.createDirectories(projectDir.resolve("data/tree"));
        Files.writeString(projectDir.resolve("data/one.txt"), "one", StandardCharsets.UTF_8);
        Files.writeString(projectDir.resolve("data/dir/two.txt"), "two", StandardCharsets.UTF_8);
        Files.writeString(projectDir.resolve("data/tree/three.csv"), "three", StandardCharsets.UTF_8);
        Files.writeString(projectDir.resolve("data/tree/ignored.txt"), "ignored", StandardCharsets.UTF_8);

        writeBuild("""
                plugins { id 'ch.so.agi.gretl' }

                import ch.so.agi.gretl.tasks.S3Upload
                import ch.so.agi.gretl.tasks.S3Bucket2Bucket
                import ch.so.agi.gretl.tasks.S3Download
                import ch.so.agi.gretl.tasks.S3Delete

                def common = {
                    accessKey '%s'
                    secretKey '%s'
                    endPoint '%s'
                    region '%s'
                }

                tasks.register('uploadFile', S3Upload) {
                    common.delegate = delegate; common()
                    bucketName '%s'
                    sourceFile 'data/one.txt'
                    acl 'private'
                }

                tasks.register('uploadDirectory', S3Upload) {
                    common.delegate = delegate; common()
                    bucketName '%s'
                    sourceDir 'data/dir'
                    acl 'private'
                    contentType 'text/plain'
                    metaData([source: 'directory'])
                }

                tasks.register('uploadFileTree', S3Upload) {
                    common.delegate = delegate; common()
                    bucketName '%s'
                    sourceFiles fileTree('data/tree') { include '*.csv' }
                    acl 'private'
                    metadata([source: 'filetree'])
                }

                tasks.register('copyBuckets', S3Bucket2Bucket) {
                    dependsOn 'uploadFile', 'uploadDirectory', 'uploadFileTree'
                    accessKey '%s'
                    secretKey '%s'
                    sourceBucket '%s'
                    targetBucket '%s'
                    endPoint '%s'
                    region '%s'
                    acl 'private'
                    metaData([copied: 'true'])
                }

                tasks.register('downloadKey', S3Download) {
                    dependsOn 'copyBuckets'
                    common.delegate = delegate; common()
                    bucketName '%s'
                    key 'one.txt'
                    downloadDir layout.buildDirectory.dir('download-key')
                }

                tasks.register('downloadAll', S3Download) {
                    dependsOn 'copyBuckets'
                    common.delegate = delegate; common()
                    bucketName '%s'
                    downloadDir layout.buildDirectory.dir('download-all')
                }

                tasks.register('deleteKey', S3Delete) {
                    dependsOn 'downloadKey', 'downloadAll'
                    common.delegate = delegate; common()
                    bucketName '%s'
                    key 'one.txt'
                }

                tasks.register('deleteAll', S3Delete) {
                    dependsOn 'deleteKey'
                    common.delegate = delegate; common()
                    bucketName '%s'
                }
                """.formatted(
                FLOCI.getAccessKey(), FLOCI.getSecretKey(), FLOCI.getEndpoint(), FLOCI.getRegion(),
                sourceBucket,
                sourceBucket,
                sourceBucket,
                FLOCI.getAccessKey(), FLOCI.getSecretKey(), sourceBucket, targetBucket, FLOCI.getEndpoint(), FLOCI.getRegion(),
                targetBucket,
                targetBucket,
                targetBucket,
                targetBucket));

        run("deleteAll");

        assertEquals("one", Files.readString(projectDir.resolve("build/download-key/one.txt"), StandardCharsets.UTF_8));
        assertEquals("one", Files.readString(projectDir.resolve("build/download-all/one.txt"), StandardCharsets.UTF_8));
        assertEquals("two", Files.readString(projectDir.resolve("build/download-all/two.txt"), StandardCharsets.UTF_8));
        assertEquals("three", Files.readString(projectDir.resolve("build/download-all/three.csv"), StandardCharsets.UTF_8));

        try (S3Client s3 = s3()) {
            assertObjectText(s3, sourceBucket, "one.txt", "one");
            assertObjectText(s3, sourceBucket, "two.txt", "two");
            assertObjectText(s3, sourceBucket, "three.csv", "three");
            assertFalse(objectExists(s3, sourceBucket, "ignored.txt"));
            assertFalse(objectExists(s3, targetBucket, "one.txt"));
            assertTrue(s3.listObjectsV2(builder -> builder.bucket(targetBucket)).contents().isEmpty());

            HeadObjectResponse head = s3.headObject(builder -> builder.bucket(sourceBucket).key("two.txt"));
            assertEquals("text/plain", head.contentType());
            assertEquals("directory", head.metadata().get("source"));
        }
    }

    @Test
    void failsWhenBucketIsMissing() throws Exception {
        writeSettings();
        Files.createDirectories(projectDir.resolve("data"));
        Files.writeString(projectDir.resolve("data/one.txt"), "one", StandardCharsets.UTF_8);
        writeBuild("""
                plugins { id 'ch.so.agi.gretl' }

                import ch.so.agi.gretl.tasks.S3Upload

                tasks.register('uploadFile', S3Upload) {
                    accessKey '%s'
                    secretKey '%s'
                    endpoint '%s'
                    region '%s'
                    bucketName '%s'
                    sourceFile 'data/one.txt'
                    acl 'private'
                }
                """.formatted(FLOCI.getAccessKey(), FLOCI.getSecretKey(), FLOCI.getEndpoint(),
                FLOCI.getRegion(), bucket("missing")));

        BuildResult result = runAndFail("uploadFile");

        assertTrue(result.getOutput().contains("S3Upload"));
    }

    private S3Client s3() {
        return S3Client.builder()
                .endpointOverride(URI.create(FLOCI.getEndpoint()))
                .region(Region.of(FLOCI.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(FLOCI.getAccessKey(), FLOCI.getSecretKey())))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    private void assertObjectText(S3Client s3, String bucket, String key, String expected) {
        ResponseBytes<GetObjectResponse> bytes = s3.getObjectAsBytes(builder -> builder.bucket(bucket).key(key));
        assertEquals(expected, bytes.asUtf8String());
    }

    private boolean objectExists(S3Client s3, String bucket, String key) {
        try {
            s3.headObject(builder -> builder.bucket(bucket).key(key));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String bucket(String prefix) {
        return "gretl-" + prefix + "-" + UUID.randomUUID().toString().replace("-", "");
    }

    private BuildResult run(String... arguments) {
        return GretlBuildExecutors.current().run(projectDir, arguments);
    }

    private BuildResult runAndFail(String... arguments) {
        return GretlBuildExecutors.current().runAndFail(projectDir, arguments);
    }

    private void writeSettings() throws IOException {
        GretlTestProjectSettings.write(projectDir, "s3-floci-test");
    }

    private void writeBuild(String content) throws IOException {
        Files.writeString(projectDir.resolve("build.gradle"), content, StandardCharsets.UTF_8);
    }

}
