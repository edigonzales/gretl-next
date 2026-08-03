package ch.so.agi.gretl.test.fixture;

import ch.so.agi.gretl.test.job.TestJobExecutionIdentity;
import ch.so.agi.gretl.test.job.TestJobExecutionTarget;
import org.apache.commons.net.ftp.FTPClient;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.util.Map;
import java.util.Optional;

public final class FtpTestFixture implements TestFixture {
    public static final String IMAGE = "docker.io/delfer/alpine-ftp-server@sha256:60bb774d8408d9d4d5c74d05d1c086a34ce192c6c1a142ffac268cac0dbc6fac";
    private FixedHostPortGenericContainer<?> container;
    private int passivePort;
    private TestFixtureNetwork network;
    private boolean closed;

    @Override public TestFixtureType type() { return TestFixtureType.FTP; }
    @Override public synchronized void start(TestFixtureStartContext context) {
        if (closed) throw new IllegalStateException("FTP fixture is closed");
        if (this.network != null) return;
        TestFixtureNetwork network = context.requireNetwork(type());
        new FixedHostPortAllocator().executeWithRetry(3, port -> {
            FixedHostPortGenericContainer<?> candidate = createContainer(network, port);
            try {
                candidate.start();
                container = candidate;
                passivePort = port;
                return null;
            } catch (RuntimeException failure) {
                if (candidate.isRunning()) candidate.stop();
                throw failure;
            }
        });
        this.network = network;
    }
    @Override public synchronized boolean isRunning() { return container != null && container.isRunning(); }
    @Override public synchronized TestFixtureLease acquire(TestJobExecutionIdentity identity) {
        if (!isRunning()) throw new IllegalStateException("FTP fixture is not running");
        String directory = "/ftp/user/" + identity.shortToken();
        createDirectory(directory);
        return new FtpTestFixtureLease(this, identity.namespace(), directory, "user", "password");
    }

    TestFixtureEndpointView endpoint(String directory, String user, String password,
                                     TestJobExecutionTarget target) {
        boolean runtime = target == TestJobExecutionTarget.RUNTIME_IMAGE_ONE_SHOT
                || target == TestJobExecutionTarget.RUNTIME_IMAGE_SERVICE;
        return new TestFixtureEndpointView(Map.of(
                "server", TestFixtureValue.publicValue(runtime ? "ftp:21" : "127.0.0.1:" + container.getMappedPort(21)),
                "username", TestFixtureValue.publicValue(user),
                "password", TestFixtureValue.secretValue(password),
                "remoteDirectory", TestFixtureValue.publicValue(directory),
                "passiveMode", TestFixtureValue.publicValue(runtime ? "false" : "true")),
                runtime ? Optional.of(network.dockerNetworkId()) : Optional.empty());
    }

    void deleteDirectory(String directory) {
        if (!container.isRunning()) return;
        FTPClient ftp = connect();
        try {
            deleteRecursively(ftp, directory);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Could not clean FTP fixture lease", e);
        } finally {
            try { ftp.disconnect(); } catch (java.io.IOException ignored) { }
        }
    }

    java.util.List<String> listFiles(String directory) {
        FTPClient ftp = connect();
        try { return java.util.Arrays.stream(ftp.listFiles(directory)).map(org.apache.commons.net.ftp.FTPFile::getName).sorted().toList(); }
        catch (java.io.IOException e) { throw new IllegalStateException("Could not list FTP fixture lease", e); }
        finally { try { ftp.disconnect(); } catch (java.io.IOException ignored) { } }
    }

    boolean fileExists(String directory, String name) {
        return listFiles(directory).contains(name);
    }

    byte[] readFile(String directory, String name) {
        FTPClient ftp = connect();
        try (var output = new java.io.ByteArrayOutputStream()) {
            if (!ftp.retrieveFile(directory + "/" + name, output)) throw new IllegalStateException("FTP file does not exist: " + name);
            return output.toByteArray();
        } catch (java.io.IOException e) { throw new IllegalStateException("Could not read FTP fixture file", e); }
        finally { try { ftp.disconnect(); } catch (java.io.IOException ignored) { } }
    }

    @Override public synchronized void close() {
        if (closed) return;
        closed = true;
        if (container != null && container.isRunning()) container.stop();
    }

    private void createDirectory(String directory) {
        FTPClient ftp = connect();
        try { ftp.makeDirectory(directory); }
        catch (java.io.IOException e) { throw new IllegalStateException("Could not create FTP lease directory", e); }
        finally { try { ftp.disconnect(); } catch (java.io.IOException ignored) { } }
    }

    private FTPClient connect() {
        try {
            FTPClient ftp = new FTPClient();
            ftp.connect("127.0.0.1", container.getMappedPort(21));
            if (!ftp.login("user", "password")) throw new java.io.IOException("FTP login failed");
            ftp.enterLocalPassiveMode();
            return ftp;
        } catch (java.io.IOException e) { throw new IllegalStateException("Could not connect to FTP fixture", e); }
    }

    private void deleteRecursively(FTPClient ftp, String directory) throws java.io.IOException {
        for (var file : ftp.listFiles(directory)) {
            String path = directory + "/" + file.getName();
            if (file.isDirectory()) deleteRecursively(ftp, path);
            else ftp.deleteFile(path);
        }
        ftp.removeDirectory(directory);
    }

    private FixedHostPortGenericContainer<?> createContainer(TestFixtureNetwork network, int port) {
        return new FixedHostPortGenericContainer<>(IMAGE)
                .withNetwork(network.testcontainersNetwork())
                .withNetworkAliases("ftp")
                .withEnv("USERS", "user|password|/ftp/user")
                .withEnv("ADDRESS", "127.0.0.1")
                .withEnv("MIN_PORT", String.valueOf(port))
                .withEnv("MAX_PORT", String.valueOf(port))
                .withExposedPorts(21)
                .withFixedExposedPort(port, port)
                .withCreateContainerCmdModifier(command -> command.withHostName("ftp").withAliases("ftp"))
                .waitingFor(Wait.forListeningPort());
    }
}
