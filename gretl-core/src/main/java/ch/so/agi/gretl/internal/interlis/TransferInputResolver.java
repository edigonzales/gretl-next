package ch.so.agi.gretl.internal.interlis;

import ch.so.agi.gretl.tasks.AbstractIli2DbTransferTask;
import org.gradle.api.GradleException;

import java.io.File;
import java.util.List;

final class TransferInputResolver {

    TransferInputs resolve(AbstractIli2DbTransferTask task, boolean allowRepositoryIds) {
        List<String> localFiles = task.getTransferFilesCollection().getFiles().stream()
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

    record TransferInputs(List<String> localFiles, List<String> repositoryIds) {
        boolean isEmpty() {
            return localFiles.isEmpty() && repositoryIds.isEmpty();
        }

        boolean usesLocalFiles() {
            return !localFiles.isEmpty();
        }

        List<String> executionInputs() {
            return usesLocalFiles() ? localFiles : repositoryIds;
        }
    }
}
