package ch.so.agi.gretl.test.fixture;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Objects;
import java.util.function.IntFunction;

/** Allocates a new host port for each narrowly scoped fixed-port retry. */
public final class FixedHostPortAllocator {
    public <T> T executeWithRetry(int maximumAttempts, IntFunction<T> operation) {
        if (maximumAttempts < 1) throw new IllegalArgumentException("maximumAttempts must be positive");
        Objects.requireNonNull(operation, "operation must not be null");
        RuntimeException previousCollision = null;
        for (int attempt = 1; attempt <= maximumAttempts; attempt++) {
            int port = allocatePort();
            try {
                return operation.apply(port);
            } catch (RuntimeException failure) {
                if (!isPortCollision(failure) || attempt == maximumAttempts) {
                    if (previousCollision != null) failure.addSuppressed(previousCollision);
                    throw failure;
                }
                if (previousCollision == null) previousCollision = failure;
                else previousCollision.addSuppressed(failure);
            }
        }
        throw new IllegalStateException("Fixed host port allocation failed");
    }

    private int allocatePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException("Could not allocate a host port", e);
        }
    }

    private boolean isPortCollision(Throwable failure) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            String message = current.getMessage();
            if (message != null && (message.toLowerCase(java.util.Locale.ROOT).contains("port is already allocated")
                    || message.toLowerCase(java.util.Locale.ROOT).contains("address already in use")
                    || message.toLowerCase(java.util.Locale.ROOT).contains("bind failed")
                    || message.toLowerCase(java.util.Locale.ROOT).contains("port collision"))) {
                return true;
            }
        }
        return false;
    }
}
