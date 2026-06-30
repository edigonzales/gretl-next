package ch.so.agi.gretl.internal.interlis;

import ch.so.agi.gretl.tasks.AbstractIli2DbFileTask;
import ch.so.agi.gretl.tasks.AbstractIli2DbTransferTask;
import org.gradle.api.GradleException;

import java.io.File;
import java.util.Comparator;
import java.util.List;

public final class TransferInputResolver {

    public TransferInputs resolve(AbstractIli2DbTransferTask task, boolean allowRepositoryIds) {
        List<String> localFiles = task.getTransferFilesCollection().getFiles().stream()
                .sorted(Comparator.comparing(File::getPath))
                .map(File::getAbsolutePath)
                .toList();
        List<String> repositoryIds = task.getRepositoryDataIds().get();

        if (!allowRepositoryIds && !repositoryIds.isEmpty()) {
            throw new GradleException("repositoryDataIds are only supported for import tasks");
        }
        if (!localFiles.isEmpty() && !repositoryIds.isEmpty()) {
            throw new GradleException("Use either transferFiles(...) or repositoryDataIds(...), not both.");
        }
        return new TransferInputs(localFiles, repositoryIds);
    }

    public TransferInputs resolveLocal(AbstractIli2DbFileTask task) {
        List<String> localFiles = task.getTransferFilesCollection().getFiles().stream()
                .sorted(Comparator.comparing(File::getPath))
                .map(File::getAbsolutePath)
                .toList();
        return new TransferInputs(localFiles, List.of());
    }

    public record TransferInputs(List<String> localFiles, List<String> repositoryIds) {
        public boolean isEmpty() {
            return localFiles.isEmpty() && repositoryIds.isEmpty();
        }

        public boolean usesLocalFiles() {
            return !localFiles.isEmpty();
        }

        public List<String> executionInputs() {
            return usesLocalFiles() ? localFiles : repositoryIds;
        }
    }
}
