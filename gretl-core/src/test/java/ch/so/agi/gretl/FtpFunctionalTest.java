package ch.so.agi.gretl;

import org.junit.jupiter.api.Test;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileEntry;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;

import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FtpFunctionalTest extends CoreFunctionalTestSupport {

    @Test
    void uploadsDownloadsDeletesAndListsFiles() throws Exception {
        writeSettings();
        Files.createDirectories(projectDir.resolve("local"));
        Files.writeString(projectDir.resolve("local/upload.txt"), "uploaded", StandardCharsets.UTF_8);

        try (RunningFtpServer ftp = RunningFtpServer.start()) {
            writeBuild("""
                    plugins { id 'ch.so.agi.gretl' }

                    import ch.so.agi.gretl.tasks.FtpUpload
                    import ch.so.agi.gretl.tasks.FtpDownload
                    import ch.so.agi.gretl.tasks.FtpDelete
                    import ch.so.agi.gretl.tasks.FtpList

                    def ftpServer = 'localhost:%d'

                    tasks.register('uploadFile', FtpUpload) {
                        server ftpServer
                        user 'user'
                        password 'password'
                        remoteDir '/remote'
                        localFile 'local/upload.txt'
                        fileType 'BINARY'
                    }

                    tasks.register('downloadPattern', FtpDownload) {
                        dependsOn 'uploadFile'
                        server ftpServer
                        user 'user'
                        password 'password'
                        remoteDir '/remote'
                        remoteFile '*.txt'
                        localDir layout.buildDirectory.dir('download-pattern').get().asFile
                        fileType 'BINARY'
                    }

                    tasks.register('downloadSingle', FtpDownload) {
                        dependsOn 'uploadFile'
                        server ftpServer
                        user 'user'
                        password 'password'
                        remoteDir '/remote'
                        remoteFile 'keep.txt'
                        localDir layout.buildDirectory.dir('download-single').get().asFile
                        fileType 'BINARY'
                    }

                    tasks.register('downloadList', FtpDownload) {
                        dependsOn 'uploadFile'
                        server ftpServer
                        user 'user'
                        password 'password'
                        remoteDir '/remote'
                        remoteFile(['old-one.txt', 'keep.txt'])
                        localDir layout.buildDirectory.dir('download-list').get().asFile
                        fileType 'BINARY'
                    }

                    tasks.register('downloadAll', FtpDownload) {
                        dependsOn 'uploadFile'
                        server ftpServer
                        user 'user'
                        password 'password'
                        remoteDir '/remote'
                        localDir layout.buildDirectory.dir('download-all').get().asFile
                        fileType 'BINARY'
                    }

                    tasks.register('deleteSingle', FtpDelete) {
                        dependsOn 'downloadPattern', 'downloadSingle', 'downloadList', 'downloadAll'
                        server ftpServer
                        user 'user'
                        password 'password'
                        remoteDir '/remote'
                        remoteFile 'delete-me.txt'
                    }

                    tasks.register('deleteOldFiles', FtpDelete) {
                        dependsOn 'deleteSingle'
                        server ftpServer
                        user 'user'
                        password 'password'
                        remoteDir '/remote'
                        remoteFile 'old-*'
                    }

                    tasks.register('listRemote', FtpList) {
                        dependsOn 'deleteOldFiles'
                        server ftpServer
                        user 'user'
                        password 'password'
                        remoteDir '/remote'
                    }

                    tasks.register('writeList') {
                        dependsOn 'listRemote'
                        doLast {
                            file("$buildDir/list.txt").text = tasks.named('listRemote').get().files.sort().join(',')
                        }
                    }

                    tasks.register('deleteAll', FtpDelete) {
                        dependsOn 'writeList'
                        server ftpServer
                        user 'user'
                        password 'password'
                        remoteDir '/remote'
                    }
                    """.formatted(ftp.port()));

            run("deleteAll");

            UnixFakeFileSystem fileSystem = ftp.fileSystem();
            assertFalse(fileSystem.exists("/remote/old-one.txt"));
            assertFalse(fileSystem.exists("/remote/old-two.txt"));
            assertFalse(fileSystem.exists("/remote/delete-me.txt"));
            assertFalse(fileSystem.exists("/remote/keep.txt"));
            assertFalse(fileSystem.exists("/remote/upload.txt"));
            assertEquals("old", Files.readString(projectDir.resolve("build/download-pattern/old-one.txt"), StandardCharsets.UTF_8));
            assertEquals("keep", Files.readString(projectDir.resolve("build/download-single/keep.txt"), StandardCharsets.UTF_8));
            assertEquals("old", Files.readString(projectDir.resolve("build/download-list/old-one.txt"), StandardCharsets.UTF_8));
            assertEquals("keep", Files.readString(projectDir.resolve("build/download-list/keep.txt"), StandardCharsets.UTF_8));
            assertEquals("uploaded", Files.readString(projectDir.resolve("build/download-all/upload.txt"), StandardCharsets.UTF_8));
            assertEquals("keep.txt,upload.txt", Files.readString(projectDir.resolve("build/list.txt"), StandardCharsets.UTF_8));
        }
    }

    private record RunningFtpServer(FakeFtpServer server, UnixFakeFileSystem fileSystem, int port)
            implements AutoCloseable {

        static RunningFtpServer start() throws Exception {
            int port = freePort();
            FakeFtpServer server = new FakeFtpServer();
            server.setServerControlPort(port);
            server.addUserAccount(new UserAccount("user", "password", "/"));

            UnixFakeFileSystem fileSystem = new UnixFakeFileSystem();
            fileSystem.add(new DirectoryEntry("/"));
            fileSystem.add(new DirectoryEntry("/remote"));
            fileSystem.add(new FileEntry("/remote/old-one.txt", "old"));
            fileSystem.add(new FileEntry("/remote/old-two.txt", "old"));
            fileSystem.add(new FileEntry("/remote/delete-me.txt", "delete"));
            fileSystem.add(new FileEntry("/remote/keep.txt", "keep"));
            server.setFileSystem(fileSystem);
            server.start();
            return new RunningFtpServer(server, fileSystem, port);
        }

        @Override
        public void close() {
            server.stop();
        }

        private static int freePort() throws Exception {
            try (ServerSocket socket = new ServerSocket(0)) {
                return socket.getLocalPort();
            }
        }
    }
}
