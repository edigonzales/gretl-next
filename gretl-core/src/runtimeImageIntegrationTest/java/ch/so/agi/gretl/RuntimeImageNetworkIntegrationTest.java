package ch.so.agi.gretl;

import ch.so.agi.gretl.test.docker.ContainerUserResolver;
import ch.so.agi.gretl.test.docker.DockerCli;
import ch.so.agi.gretl.test.execution.GretlBuildRequest;
import ch.so.agi.gretl.test.execution.GretlBuildResult;
import ch.so.agi.gretl.test.execution.RuntimeImageBuildExecutor;
import ch.so.agi.gretl.test.execution.RuntimeImageLifecycleArguments;
import ch.so.agi.gretl.test.runtime.RuntimeImageDescriptor;
import ch.so.agi.gretl.test.runtime.RuntimeImageRunOptions;
import io.floci.testcontainers.FlociContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("runtime-image")
@Tag("runtime-image-smoke")
class RuntimeImageNetworkIntegrationTest {
    private static final Network NETWORK = Network.newNetwork();
    private static final String FTP_IMAGE =
            "docker.io/delfer/alpine-ftp-server@sha256:60bb774d8408d9d4d5c74d05d1c086a34ce192c6c1a142ffac268cac0dbc6fac";

    @Container
    private static final GenericContainer<?> HTTP = new GenericContainer<>("python:3.12-alpine")
            .withNetwork(NETWORK)
            .withNetworkAliases("http")
            .withCopyFileToContainer(
                    MountableFile.forClasspathResource("runtime-http/server.py"), "/server.py")
            .withExposedPorts(8080)
            .withCommand("python", "/server.py")
            .waitingFor(Wait.forHttp("/health").forPort(8080).withStartupTimeout(Duration.ofSeconds(45)));

    @Container
    private static final GenericContainer<?> FTP = new GenericContainer<>(FTP_IMAGE)
            .withNetwork(NETWORK)
            .withNetworkAliases("ftp")
            .withCreateContainerCmdModifier(command -> command.withHostName("ftp").withAliases("ftp"))
            .withEnv("USERS", "user|password|/ftp/user")
            .withEnv("ADDRESS", "ftp")
            .withEnv("MIN_PORT", "21000")
            .withEnv("MAX_PORT", "21000")
            // The passive port is reachable directly over the shared Docker
            // network; exposing it to the host would make readiness depend on
            // a port that only opens after the first FTP data connection.
            .withExposedPorts(21)
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(45)));

    @Container
    private static final FlociContainer S3 = new FlociContainer()
            .withRegion("us-east-1")
            .withNetwork(NETWORK)
            .withNetworkAliases("s3");

    @TempDir
    Path project;

    @BeforeAll
    void startServices() {
        // The @Container fields are started by the Testcontainers extension.
        assertTrue(HTTP.isRunning());
        assertTrue(FTP.isRunning());
        assertTrue(S3.isRunning());
    }

    @AfterAll
    void closeNetwork() {
        NETWORK.close();
    }

    @Test
    void executesCurlAgainstContainerHttpServiceWithoutLeakingSecret() throws Exception {
        Files.createDirectories(project.resolve("data"));
        Files.writeString(project.resolve("settings.gradle"), "rootProject.name = 'runtime-http'\n");
        Files.writeString(project.resolve("data/payload.txt"), "binary-content", StandardCharsets.UTF_8);
        Files.writeString(project.resolve("build.gradle"), """
                plugins { id 'ch.so.agi.gretl' }
                import ch.so.agi.gretl.tasks.Curl

                tasks.register('postText', Curl) {
                    serverUrl 'http://http:8080/text'
                    method Curl.MethodType.POST
                    expectedStatusCode 201
                    expectedBody 'accepted'
                    user 'reader'
                    password 'http-secret'
                    data 'hello-text'
                }
                tasks.register('postBinary', Curl) {
                    serverUrl 'http://http:8080/binary'
                    method Curl.MethodType.POST
                    expectedStatusCode 200
                    expectedBody 'ok'
                    dataBinary 'data/payload.txt'
                }
                tasks.register('postForm', Curl) {
                    serverUrl 'http://http:8080/form'
                    method Curl.MethodType.POST
                    expectedStatusCode 200
                    expectedBody 'form-ok'
                    formData([payload: file('data/payload.txt')])
                }
                tasks.register('download', Curl) {
                    serverUrl 'http://http:8080/download'
                    expectedStatusCode 200
                    outputFile layout.buildDirectory.file('download.txt')
                }
                """, StandardCharsets.UTF_8);

        GretlBuildResult result = execute(List.of("postText", "postBinary", "postForm", "download"));

        assertTrue(result.successful(), result.output());
        assertFalse(result.output().contains("http-secret"));
        assertEquals("download-content",
                Files.readString(project.resolve("build/download.txt"), StandardCharsets.UTF_8));
    }

    @Test
    void executesFtpUploadListDownloadAndDeleteInsideNetwork() throws Exception {
        Files.createDirectories(project.resolve("data"));
        Files.writeString(project.resolve("settings.gradle"), "rootProject.name = 'runtime-ftp'\n");
        Files.write(project.resolve("data/payload.bin"), new byte[]{0, 1, 2, 3, (byte) 255});
        Files.writeString(project.resolve("build.gradle"), """
                plugins { id 'ch.so.agi.gretl' }
                import ch.so.agi.gretl.tasks.FtpUpload
                import ch.so.agi.gretl.tasks.FtpList
                import ch.so.agi.gretl.tasks.FtpDownload
                import ch.so.agi.gretl.tasks.FtpDelete

                def common = {
                    server 'ftp:21'
                    user 'user'
                    password 'password'
                    remoteDir '/ftp/user'
                }
                tasks.register('upload', FtpUpload) {
                    common.delegate = delegate; common()
                    fileType 'BINARY'
                    localFile 'data/payload.bin'
                    remoteDir '/ftp/user'
                }
                tasks.register('list', FtpList) {
                    dependsOn 'upload'
                    common.delegate = delegate; common()
                    remoteDir '/ftp/user'
                }
                tasks.register('download', FtpDownload) {
                    dependsOn 'upload'
                    common.delegate = delegate; common()
                    fileType 'BINARY'
                    remoteFile 'payload.bin'
                    localDir layout.buildDirectory.dir('download')
                }
                tasks.register('delete', FtpDelete) {
                    dependsOn 'download', 'list'
                    common.delegate = delegate; common()
                    remoteFile 'payload.bin'
                    remoteDir '/ftp/user'
                }
                tasks.register('verify') {
                    dependsOn 'list', 'delete'
                    doLast {
                        file('build/list.txt').text = tasks.named('list').get().files.sort().join(',')
                    }
                }
                """, StandardCharsets.UTF_8);

        GretlBuildResult result = execute(List.of("verify"), "password");

        assertTrue(result.successful(), result.output());
        assertFalse(result.output().contains("password"));
        assertEquals("payload.bin", Files.readString(project.resolve("build/list.txt"), StandardCharsets.UTF_8));
        assertArrayEquals(new byte[]{0, 1, 2, 3, (byte) 255},
                Files.readAllBytes(project.resolve("build/download/payload.bin")));
    }

    @Test
    void executesS3UploadCopyDownloadAndDeleteInsideNetwork() throws Exception {
        String sourceBucket = bucket("source");
        String targetBucket = bucket("target");
        try (var s3 = hostS3()) {
            s3.createBucket(builder -> builder.bucket(sourceBucket));
            s3.createBucket(builder -> builder.bucket(targetBucket));
        }
        Files.createDirectories(project.resolve("data"));
        Files.writeString(project.resolve("settings.gradle"), "rootProject.name = 'runtime-s3'\n");
        Files.writeString(project.resolve("data/one.txt"), "one", StandardCharsets.UTF_8);
        Files.writeString(project.resolve("build.gradle"), """
                plugins { id 'ch.so.agi.gretl' }
                import ch.so.agi.gretl.tasks.S3Upload
                import ch.so.agi.gretl.tasks.S3Bucket2Bucket
                import ch.so.agi.gretl.tasks.S3Download
                import ch.so.agi.gretl.tasks.S3Delete

                def common = {
                    accessKey '%s'
                    secretKey '%s'
                    endpoint 'http://s3:4566'
                    region '%s'
                }
                tasks.register('upload', S3Upload) {
                    common.delegate = delegate; common()
                    bucketName '%s'
                    sourceFile 'data/one.txt'
                    acl 'private'
                }
                tasks.register('copy', S3Bucket2Bucket) {
                    dependsOn 'upload'
                    accessKey '%s'; secretKey '%s'
                    sourceBucket '%s'; targetBucket '%s'
                    endPoint 'http://s3:4566'; region '%s'; acl 'private'
                }
                tasks.register('download', S3Download) {
                    dependsOn 'copy'
                    common.delegate = delegate; common()
                    bucketName '%s'; key 'one.txt'
                    downloadDir layout.buildDirectory.dir('download')
                }
                tasks.register('delete', S3Delete) {
                    dependsOn 'download'
                    common.delegate = delegate; common()
                    bucketName '%s'; key 'one.txt'
                }
                """.formatted(
                S3.getAccessKey(), S3.getSecretKey(), S3.getRegion(), sourceBucket,
                S3.getAccessKey(), S3.getSecretKey(), sourceBucket, targetBucket, S3.getRegion(),
                targetBucket, targetBucket), StandardCharsets.UTF_8);

        GretlBuildResult result = execute(List.of("delete"), S3.getAccessKey(), S3.getSecretKey());

        assertTrue(result.successful(), result.output());
        assertEquals("one", Files.readString(project.resolve("build/download/one.txt"), StandardCharsets.UTF_8));
        try (var s3 = hostS3()) {
            assertFalse(objectExists(s3, targetBucket, "one.txt"));
        }
    }

    @Test
    void executesIli2duckdbSchemaImportImportAndExport() throws Exception {
        copyResource("fixtures/interlis/ili2duckdb/import/KS3-20060703.ili", "KS3-20060703.ili");
        copyResource("fixtures/interlis/ili2duckdb/import/VOLLZUG_SO0200002401_1531_20180105113131.xml",
                "transfer.xml");
        Files.writeString(project.resolve("settings.gradle"), "rootProject.name = 'runtime-interlis'\n");
        Files.writeString(project.resolve("build.gradle"), """
                plugins { id 'ch.so.agi.gretl' }
                import ch.so.agi.gretl.tasks.Ili2duckdbImportSchema
                import ch.so.agi.gretl.tasks.Ili2duckdbImport
                import ch.so.agi.gretl.tasks.Ili2duckdbExport

                def modelDir = projectDir.toString()
                tasks.register('schema', Ili2duckdbImportSchema) {
                    databaseFile layout.buildDirectory.file('db/data.duckdb')
                    modelNames 'GB2AV'; modelDirectories modelDir
                    schema 'gb2av'; coalesceJson.set(true); createBasketCol.set(true)
                }
                tasks.register('importData', Ili2duckdbImport) {
                    dependsOn 'schema'
                    databaseFile layout.buildDirectory.file('db/data.duckdb')
                    modelNames 'GB2AV'; modelDirectories modelDir; schema 'gb2av'
                    transferFiles 'transfer.xml'
                }
                tasks.register('exportData', Ili2duckdbExport) {
                    dependsOn 'importData'
                    databaseFile layout.buildDirectory.file('db/data.duckdb')
                    modelNames 'GB2AV'; modelDirectories modelDir; schema 'gb2av'
                    dataFiles layout.buildDirectory.file('out/export.xml')
                }
                """, StandardCharsets.UTF_8);

        GretlBuildResult result = execute(List.of("exportData"));

        assertTrue(result.successful(), result.output());
        assertTrue(Files.size(project.resolve("build/out/export.xml")) > 0);
        assertTrue(Files.readString(project.resolve("build/out/export.xml"), StandardCharsets.UTF_8)
                .contains("INTERLIS"));
    }

    private GretlBuildResult execute(List<String> arguments, String... secrets) {
        RuntimeImageBuildExecutor executor = new RuntimeImageBuildExecutor(
                RuntimeImageDescriptor.fromSystemProperties(), new DockerCli(),
                new ContainerUserResolver(), new RuntimeImageLifecycleArguments());
        GretlBuildRequest.Builder request = GretlBuildRequest.builder(project)
                .arguments(arguments)
                .timeout(Duration.ofMinutes(10))
                .runtimeImageOptions(RuntimeImageRunOptions.onNetwork(NETWORK.getId()));
        for (String secret : secrets) {
            request.secret(secret);
        }
        return executor.execute(request.build());
    }

    private void copyResource(String resource, String target) throws Exception {
        try (var input = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertTrue(input != null, "Missing test resource " + resource);
            Path destination = project.resolve(target);
            Files.createDirectories(destination.getParent() == null ? project : destination.getParent());
            Files.copy(input, destination);
        }
    }

    private software.amazon.awssdk.services.s3.S3Client hostS3() {
        return software.amazon.awssdk.services.s3.S3Client.builder()
                .endpointOverride(URI.create(S3.getEndpoint()))
                .region(software.amazon.awssdk.regions.Region.of(S3.getRegion()))
                .credentialsProvider(software.amazon.awssdk.auth.credentials.StaticCredentialsProvider.create(
                        software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create(
                                S3.getAccessKey(), S3.getSecretKey())))
                .serviceConfiguration(software.amazon.awssdk.services.s3.S3Configuration.builder()
                        .pathStyleAccessEnabled(true).build())
                .build();
    }

    private boolean objectExists(software.amazon.awssdk.services.s3.S3Client s3, String bucket, String key) {
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

}
