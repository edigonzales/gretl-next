package ch.so.agi.gretl.internal.ftp;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class FtpEngine {

    public void upload(FtpUploadRequest request) throws Exception {
        validateConnection(request.connection());
        requireNotNull(request.localFile(), "localFile must not be null");
        requireNotBlank(request.remoteDir(), "remoteDir must not be null");
        try (FtpSession session = open(request.connection())) {
            setFileType(session.client(), request.fileType());
            String remotePath = join(request.remoteDir(), request.localFile().getFileName().toString(), session.fileSeparator());
            try (InputStream in = Files.newInputStream(request.localFile())) {
                if (!session.client().storeFile(remotePath, in)) {
                    throw new IOException("Could not upload the file: " + remotePath);
                }
            }
        }
    }

    public void download(FtpDownloadRequest request) throws Exception {
        validateConnection(request.connection());
        requireNotNull(request.localDir(), "localDir must not be null");
        requireNotBlank(request.remoteDir(), "remoteDir must not be null");
        Files.createDirectories(request.localDir());
        try (FtpSession session = open(request.connection())) {
            setFileType(session.client(), request.fileType());
            List<String> fileNames = request.remoteFiles() == null || request.remoteFiles().isEmpty()
                    ? listRemoteFiles(session.client(), request.remoteDir())
                    : expandPatterns(session.client(), request.remoteDir(), request.remoteFiles());
            for (String fileName : fileNames) {
                downloadOne(session.client(), request.remoteDir(), fileName, request.localDir(), session.fileSeparator());
            }
        }
    }

    public void delete(FtpDeleteRequest request) throws Exception {
        validateConnection(request.connection());
        requireNotBlank(request.remoteDir(), "remoteDir must not be null");
        try (FtpSession session = open(request.connection())) {
            List<String> fileNames = request.remoteFiles() == null || request.remoteFiles().isEmpty()
                    ? listRemoteFiles(session.client(), request.remoteDir())
                    : expandPatterns(session.client(), request.remoteDir(), request.remoteFiles());
            for (String fileName : fileNames) {
                session.client().deleteFile(join(request.remoteDir(), fileName, session.fileSeparator()));
            }
        }
    }

    public List<String> list(FtpListRequest request) throws Exception {
        validateConnection(request.connection());
        requireNotBlank(request.remoteDir(), "remoteDir must not be null");
        try (FtpSession session = open(request.connection())) {
            return listRemoteFiles(session.client(), request.remoteDir());
        }
    }

    private static FtpSession open(FtpConnectionSpec spec) throws Exception {
        FTPClient ftp = new FTPClient();
        ftp.configure(new FTPClientConfig(defaultString(spec.systemType(), FTPClientConfig.SYST_UNIX)));

        HostAndPort hostAndPort = parseServer(spec.server());
        ftp.connect(hostAndPort.host(), hostAndPort.port());
        ftp.login(spec.user(), spec.password());

        int reply = ftp.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)) {
            disconnectQuietly(ftp);
            throw new IOException("FTP server refused connection.");
        }
        if (spec.passiveMode()) {
            ftp.enterLocalPassiveMode();
        } else {
            ftp.enterLocalActiveMode();
        }
        if (spec.controlKeepAliveTimeout() > 0) {
            ftp.setControlKeepAliveTimeout(spec.controlKeepAliveTimeout());
        }
        String separator = spec.fileSeparator();
        if (separator == null || separator.isEmpty()) {
            separator = defaultString(spec.systemType(), FTPClientConfig.SYST_UNIX)
                    .equalsIgnoreCase(FTPClientConfig.SYST_NT) ? "\\" : "/";
        }
        return new FtpSession(ftp, separator);
    }

    private static void setFileType(FTPClient ftp, FtpFileType fileType) throws IOException {
        if (fileType == FtpFileType.BINARY) {
            ftp.setFileType(FTP.BINARY_FILE_TYPE);
        } else {
            ftp.setFileType(FTP.ASCII_FILE_TYPE);
        }
    }

    private static List<String> listRemoteFiles(FTPClient ftp, String remoteDir) throws IOException {
        List<String> names = new ArrayList<>();
        for (FTPFile file : ftp.listFiles(remoteDir)) {
            if (file.isFile()) {
                names.add(file.getName());
            }
        }
        return names;
    }

    private static List<String> expandPatterns(FTPClient ftp, String remoteDir, List<String> candidates) throws IOException {
        List<String> result = new ArrayList<>();
        List<String> remoteNames = null;
        for (String candidate : candidates) {
            if (containsWildcard(candidate)) {
                if (remoteNames == null) {
                    remoteNames = listRemoteFiles(ftp, remoteDir);
                }
                Pattern pattern = wildcardPattern(candidate);
                for (String remoteName : remoteNames) {
                    if (pattern.matcher(remoteName).matches()) {
                        result.add(remoteName);
                    }
                }
            } else {
                result.add(candidate);
            }
        }
        return result;
    }

    private static void downloadOne(FTPClient ftp, String remoteDir, String remoteFileName, Path localDir,
                                    String separator) throws IOException {
        Path target = localDir.resolve(remoteFileName);
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        String remotePath = join(remoteDir, remoteFileName, separator);
        try (OutputStream out = Files.newOutputStream(target)) {
            if (!ftp.retrieveFile(remotePath, out)) {
                throw new IOException("Could not retrieve file: " + remotePath);
            }
        }
    }

    private static String join(String directory, String fileName, String separator) {
        if (directory.endsWith(separator)) {
            return directory + fileName;
        }
        return directory + separator + fileName;
    }

    private static boolean containsWildcard(String value) {
        return value.contains("*") || value.contains("?");
    }

    private static Pattern wildcardPattern(String wildcard) {
        StringBuilder regex = new StringBuilder();
        for (char c : wildcard.toCharArray()) {
            if (c == '*') {
                regex.append(".*");
            } else if (c == '?') {
                regex.append('.');
            } else {
                regex.append(Pattern.quote(String.valueOf(c)));
            }
        }
        return Pattern.compile(regex.toString());
    }

    private static void validateConnection(FtpConnectionSpec spec) {
        requireNotNull(spec, "connection must not be null");
        requireNotBlank(spec.server(), "server must not be null");
        requireNotBlank(spec.user(), "user must not be null");
        requireNotBlank(spec.password(), "password must not be null");
    }

    private static void requireNotNull(Object value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private static String defaultString(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static HostAndPort parseServer(String server) throws SocketException {
        Objects.requireNonNull(server, "server must not be null");
        int colon = server.lastIndexOf(':');
        if (colon > 0 && colon < server.length() - 1 && server.indexOf(']') < colon) {
            String port = server.substring(colon + 1);
            if (port.chars().allMatch(Character::isDigit)) {
                return new HostAndPort(server.substring(0, colon), Integer.parseInt(port));
            }
        }
        return new HostAndPort(server, 21);
    }

    private static void disconnectQuietly(FTPClient ftp) {
        try {
            ftp.disconnect();
        } catch (IOException ignored) {
            // Best-effort cleanup only.
        }
    }

    private record HostAndPort(String host, int port) {
    }

    private record FtpSession(FTPClient client, String fileSeparator) implements AutoCloseable {
        @Override
        public void close() throws IOException {
            if (client.isConnected()) {
                client.disconnect();
            }
        }
    }
}
