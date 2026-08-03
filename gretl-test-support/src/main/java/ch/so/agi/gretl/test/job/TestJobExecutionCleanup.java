package ch.so.agi.gretl.test.job;

import ch.so.agi.gretl.test.fixture.PreparedTestJobEnvironment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class TestJobExecutionCleanup {
    RuntimeException cleanup(MaterializedTestJob job,
                             PreparedTestJobEnvironment prepared,
                             MaterializedJobRetentionPolicy retentionPolicy,
                             boolean successful) {
        RuntimeException failure = null;
        if (prepared != null) {
            try {
                prepared.close();
            } catch (RuntimeException e) {
                failure = combine(failure, e);
            }
        }
        if (job != null && shouldDelete(retentionPolicy, successful)) {
            try {
                deleteExecutionDirectory(job.projectDirectory());
            } catch (RuntimeException e) {
                failure = combine(failure, e);
            }
        }
        return failure;
    }

    private boolean shouldDelete(MaterializedJobRetentionPolicy policy, boolean successful) {
        return policy == MaterializedJobRetentionPolicy.DELETE_ALWAYS
                || policy == MaterializedJobRetentionPolicy.DELETE_ON_SUCCESS && successful;
    }

    private RuntimeException combine(RuntimeException first, RuntimeException next) {
        if (first == null) return next;
        first.addSuppressed(next);
        return first;
    }

    private void deleteExecutionDirectory(Path directory) {
        if (!Files.exists(directory)) return;
        try (var paths = Files.walk(directory)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new CleanupDeletionException(e);
                }
            });
        } catch (CleanupDeletionException e) {
            throw new IllegalStateException("Cannot delete materialized execution directory", e.getCause());
        } catch (IOException e) {
            throw new IllegalStateException("Cannot delete materialized execution directory", e);
        }
    }

    private static final class CleanupDeletionException extends RuntimeException {
        private CleanupDeletionException(IOException cause) { super(cause); }
    }
}
