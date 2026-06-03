package ch.so.agi.gretl.internal.interlis;

import ch.so.agi.gretl.tasks.AbstractIli2DbTransferTask;
import org.gradle.api.GradleException;

import java.io.File;
import java.util.List;

final class DatasetNameResolver {

    List<String> resolve(AbstractIli2DbTransferTask task, TransferInputResolver.TransferInputs inputs) {
        List<String> explicitNames = task.getDatasetNames().get();
        boolean deriveFromTransferFiles = task.getDatasetNamesFromTransferFiles().get();
        boolean deriveFromFiles = !task.getDatasetNameFilesCollection().isEmpty();
        int strategies = (explicitNames.isEmpty() ? 0 : 1)
                + (deriveFromTransferFiles ? 1 : 0)
                + (deriveFromFiles ? 1 : 0);

        if (strategies == 0) {
            return null;
        }
        if (strategies > 1) {
            throw new GradleException("Use either datasetNames(...), datasetNamesFromTransferFiles() or datasetNamesFromFiles(...), not more than one.");
        }

        if (!explicitNames.isEmpty()) {
            if (task.getDatasetNameSliceStart().isPresent()) {
                throw new GradleException("datasetNameSlice(...) can only be used with derived dataset names");
            }
            validateCount(inputs.executionInputs().size(), explicitNames.size());
            return explicitNames;
        }

        List<File> sourceFiles = deriveFromTransferFiles
                ? task.getTransferFilesCollection().getFiles().stream().toList()
                : task.getDatasetNameFilesCollection().getFiles().stream().toList();
        if (sourceFiles.isEmpty()) {
            throw new GradleException("No files available to derive dataset names from.");
        }
        if (!inputs.usesLocalFiles()) {
            throw new GradleException("Derived dataset names require local transfer files.");
        }

        List<String> derived = sourceFiles.stream()
                .map(File::getName)
                .map(this::stripExtension)
                .map(name -> applySlice(name, task))
                .toList();
        validateCount(inputs.executionInputs().size(), derived.size());
        return derived;
    }

    private String stripExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
    }

    private String applySlice(String value, AbstractIli2DbTransferTask task) {
        if (!task.getDatasetNameSliceStart().isPresent()) {
            return value;
        }

        int start = task.getDatasetNameSliceStart().get();
        int configuredEndExclusive = task.getDatasetNameSliceEndExclusive().getOrElse(-1);
        int endExclusive = configuredEndExclusive >= 0 ? configuredEndExclusive : value.length();
        if (start < 0 || start > value.length()) {
            throw new GradleException("datasetNameSlice start " + start + " is out of range for '" + value + "'");
        }
        if (endExclusive < start || endExclusive > value.length()) {
            throw new GradleException("datasetNameSlice end " + endExclusive + " is out of range for '" + value + "'");
        }
        return value.substring(start, endExclusive);
    }

    private void validateCount(int fileCount, int datasetCount) {
        if (fileCount != datasetCount) {
            throw new GradleException("number of dataset names (" + datasetCount + ") doesn't match number of files (" + fileCount + ")");
        }
    }
}
