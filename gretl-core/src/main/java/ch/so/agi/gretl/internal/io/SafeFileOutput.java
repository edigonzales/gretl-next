package ch.so.agi.gretl.internal.io;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Internal path containment and atomic file replacement for task outputs. */
public final class SafeFileOutput {

    public static Path resolveDescendant(Path root, String relativePath) {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path requested = Path.of(relativePath);
        if (requested.isAbsolute()) {
            throw new IllegalArgumentException("Output path must be relative: " + relativePath);
        }
        Path target = normalizedRoot.resolve(requested).normalize();
        if (target.equals(normalizedRoot) || !target.startsWith(normalizedRoot)) {
            throw new IllegalArgumentException("Output path escapes target directory: " + relativePath);
        }
        return target;
    }

    public static void writeAtomically(Path target, OutputWriter writer) throws IOException {
        Path normalizedTarget = target.toAbsolutePath().normalize();
        Path parent = normalizedTarget.getParent();
        if (parent == null) {
            throw new IllegalArgumentException("Output path has no parent directory: " + target);
        }
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, "." + normalizedTarget.getFileName() + ".", ".tmp");
        try {
            writer.write(temporary);
            moveReplacing(temporary, normalizedTarget);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void moveReplacing(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @FunctionalInterface
    public interface OutputWriter {
        void write(Path output) throws IOException;
    }

    private SafeFileOutput() {
    }
}
