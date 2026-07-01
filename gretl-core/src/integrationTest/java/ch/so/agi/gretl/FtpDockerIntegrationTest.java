package ch.so.agi.gretl;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("deprecation")
class FtpDockerIntegrationTest {

    private static final String FTP_IMAGE =
            "docker.io/delfer/alpine-ftp-server@sha256:60bb774d8408d9d4d5c74d05d1c086a34ce192c6c1a142ffac268cac0dbc6fac";
    private static final String FTP_USER = "user";
    private static final String FTP_PASSWORD = "password";
    private static final String FTP_REMOTE_DIR = "/ftp/user";
    private static final int FTP_CONTROL_PORT = 21;
    private static final int MAX_START_ATTEMPTS = 3;

    @TempDir
    Path projectDir;

    @Test
    void uploadsDownloadsDeletesAndListsFilesWithDockerFtpServer() throws Exception {
        writeSettings();
        Files.createDirectories(projectDir.resolve("local"));
        Files.writeString(projectDir.resolve("local/upload-one.txt"), "uploaded-one", StandardCharsets.UTF_8);
        Files.writeString(projectDir.resolve("local/upload-two.txt"), "uploaded-two", StandardCharsets.UTF_8);
        Files.writeString(projectDir.resolve("local/delete-me.txt"), "delete-me", StandardCharsets.UTF_8);

        try (RunningDockerFtpServer ftp = RunningDockerFtpServer.start()) {
            writeBuild("""
                    plugins { id 'ch.so.agi.gretl' }

                    import ch.so.agi.gretl.tasks.FtpUpload
                    import ch.so.agi.gretl.tasks.FtpDownload
                    import ch.so.agi.gretl.tasks.FtpDelete
                    import ch.so.agi.gretl.tasks.FtpList

                    def ftpServer = '%s:%d'
                    def remoteDirectory = '%s'

                    def ftpDefaults = {
                        server ftpServer
                        user '%s'
                        password '%s'
                        remoteDir remoteDirectory
                        fileType 'BINARY'
                    }

                    tasks.register('uploadOne', FtpUpload) {
                        ftpDefaults.delegate = delegate; ftpDefaults()
                        localFile 'local/upload-one.txt'
                    }

                    tasks.register('uploadTwo', FtpUpload) {
                        ftpDefaults.delegate = delegate; ftpDefaults()
                        localFile 'local/upload-two.txt'
                    }

                    tasks.register('uploadDeleteMe', FtpUpload) {
                        ftpDefaults.delegate = delegate; ftpDefaults()
                        localFile 'local/delete-me.txt'
                    }

                    tasks.register('downloadSingle', FtpDownload) {
                        dependsOn 'uploadOne', 'uploadTwo', 'uploadDeleteMe'
                        ftpDefaults.delegate = delegate; ftpDefaults()
                        remoteFile 'upload-one.txt'
                        localDir layout.buildDirectory.dir('download-single')
                    }

                    tasks.register('downloadList', FtpDownload) {
                        dependsOn 'uploadOne', 'uploadTwo', 'uploadDeleteMe'
                        ftpDefaults.delegate = delegate; ftpDefaults()
                        remoteFile(['upload-one.txt', 'upload-two.txt'])
                        localDir layout.buildDirectory.dir('download-list')
                    }

                    tasks.register('downloadPattern', FtpDownload) {
                        dependsOn 'uploadOne', 'uploadTwo', 'uploadDeleteMe'
                        ftpDefaults.delegate = delegate; ftpDefaults()
                        remoteFile 'upload-*.txt'
                        localDir layout.buildDirectory.dir('download-pattern')
                    }

                    tasks.register('downloadAll', FtpDownload) {
                        dependsOn 'uploadOne', 'uploadTwo', 'uploadDeleteMe'
                        ftpDefaults.delegate = delegate; ftpDefaults()
                        localDir layout.buildDirectory.dir('download-all')
                    }

                    tasks.register('deleteSingle', FtpDelete) {
                        dependsOn 'downloadSingle', 'downloadList', 'downloadPattern', 'downloadAll'
                        server ftpServer
                        user '%s'
                        password '%s'
                        remoteDir remoteDirectory
                        remoteFile 'delete-me.txt'
                    }

                    tasks.register('deletePattern', FtpDelete) {
                        dependsOn 'deleteSingle'
                        server ftpServer
                        user '%s'
                        password '%s'
                        remoteDir remoteDirectory
                        remoteFile 'upload-t*.txt'
                    }

                    tasks.register('listBeforeDeleteAll', FtpList) {
                        dependsOn 'deletePattern'
                        server ftpServer
                        user '%s'
                        password '%s'
                        remoteDir remoteDirectory
                    }

                    tasks.register('writeListBeforeDeleteAll') {
                        dependsOn 'listBeforeDeleteAll'
                        doLast {
                            file("$buildDir/list-before-delete-all.txt").text =
                                    tasks.named('listBeforeDeleteAll').get().files.sort().join(',')
                        }
                    }

                    tasks.register('deleteAll', FtpDelete) {
                        dependsOn 'writeListBeforeDeleteAll'
                        server ftpServer
                        user '%s'
                        password '%s'
                        remoteDir remoteDirectory
                    }

                    tasks.register('listAfterDeleteAll', FtpList) {
                        dependsOn 'deleteAll'
                        server ftpServer
                        user '%s'
                        password '%s'
                        remoteDir remoteDirectory
                    }

                    tasks.register('writeListAfterDeleteAll') {
                        dependsOn 'listAfterDeleteAll'
                        doLast {
                            file("$buildDir/list-after-delete-all.txt").text =
                                    tasks.named('listAfterDeleteAll').get().files.sort().join(',')
                        }
                    }
                    """.formatted(
                    ftp.host(), ftp.port(), FTP_REMOTE_DIR, FTP_USER, FTP_PASSWORD,
                    FTP_USER, FTP_PASSWORD,
                    FTP_USER, FTP_PASSWORD,
                    FTP_USER, FTP_PASSWORD,
                    FTP_USER, FTP_PASSWORD,
                    FTP_USER, FTP_PASSWORD));

            run("writeListAfterDeleteAll");

            assertEquals("uploaded-one", readBuildFile("download-single/upload-one.txt"));
            assertEquals("uploaded-one", readBuildFile("download-list/upload-one.txt"));
            assertEquals("uploaded-two", readBuildFile("download-list/upload-two.txt"));
            assertEquals("uploaded-one", readBuildFile("download-pattern/upload-one.txt"));
            assertEquals("uploaded-two", readBuildFile("download-pattern/upload-two.txt"));
            assertEquals("delete-me", readBuildFile("download-all/delete-me.txt"));
            assertEquals("uploaded-one", readBuildFile("download-all/upload-one.txt"));
            assertEquals("uploaded-two", readBuildFile("download-all/upload-two.txt"));
            assertEquals("upload-one.txt", readBuildFile("list-before-delete-all.txt"));
            assertEquals("", readBuildFile("list-after-delete-all.txt"));
        }
    }

    private String readBuildFile(String relativePath) throws IOException {
        return Files.readString(projectDir.resolve("build").resolve(relativePath), StandardCharsets.UTF_8);
    }

    private BuildResult run(String... arguments) {
        return GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments(appendStacktrace(arguments))
                .forwardOutput()
                .build();
    }

    private void writeSettings() throws IOException {
        Files.writeString(projectDir.resolve("settings.gradle"), "rootProject.name = 'ftp-docker-test'\n", StandardCharsets.UTF_8);
    }

    private void writeBuild(String content) throws IOException {
        Files.writeString(projectDir.resolve("build.gradle"), content, StandardCharsets.UTF_8);
    }

    private String[] appendStacktrace(String[] arguments) {
        String[] result = new String[arguments.length + 1];
        System.arraycopy(arguments, 0, result, 0, arguments.length);
        result[arguments.length] = "--stacktrace";
        return result;
    }

    private record RunningDockerFtpServer(FixedHostPortGenericContainer<?> container) implements AutoCloseable {

        static RunningDockerFtpServer start() throws IOException {
            RuntimeException lastFailure = null;
            for (int attempt = 1; attempt <= MAX_START_ATTEMPTS; attempt++) {
                int passivePort = freePort();
                FixedHostPortGenericContainer<?> container = new FixedHostPortGenericContainer<>(FTP_IMAGE)
                        .withExposedPorts(FTP_CONTROL_PORT)
                        // Passive FTP reports this port to the client, so random host mapping would break transfers.
                        .withFixedExposedPort(passivePort, passivePort)
                        .withEnv("USERS", FTP_USER + "|" + FTP_PASSWORD + "|" + FTP_REMOTE_DIR)
                        .withEnv("ADDRESS", "127.0.0.1")
                        .withEnv("MIN_PORT", String.valueOf(passivePort))
                        .withEnv("MAX_PORT", String.valueOf(passivePort))
                        .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(30)));

                try {
                    container.start();
                    return new RunningDockerFtpServer(container);
                } catch (RuntimeException e) {
                    lastFailure = e;
                    container.stop();
                }
            }
            throw lastFailure;
        }

        String host() {
            return container.getHost();
        }

        int port() {
            return container.getMappedPort(FTP_CONTROL_PORT);
        }

        @Override
        public void close() {
            container.stop();
        }

        private static int freePort() throws IOException {
            try (ServerSocket socket = new ServerSocket(0)) {
                return socket.getLocalPort();
            }
        }
    }
}
