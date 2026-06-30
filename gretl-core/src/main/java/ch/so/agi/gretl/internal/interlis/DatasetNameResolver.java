package ch.so.agi.gretl.internal.interlis;

import ch.so.agi.gretl.tasks.AbstractIli2DbTask;
import ch.so.agi.gretl.tasks.AbstractIli2DbFileTask;
import ch.so.agi.gretl.tasks.AbstractIli2DbTransferTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class DatasetNameResolver {

    public List<String> resolve(AbstractIli2DbTask task) {
        return resolve(task, null);
    }

    public List<String> resolve(AbstractIli2DbTransferTask task, TransferInputResolver.TransferInputs inputs) {
        return resolve((AbstractIli2DbTask) task, inputs);
    }

    public List<String> resolve(AbstractIli2DbFileTask task, TransferInputResolver.TransferInputs inputs) {
        return resolve((AbstractIli2DbTask) task, inputs);
    }

    private List<String> resolve(AbstractIli2DbTask task, TransferInputResolver.TransferInputs inputs) {
        List<String> explicitNames = task.getDatasetNames().get();
        Object legacyDataset = task.getDatasetRaw();
        boolean hasLegacyDataset = legacyDataset != null;
        boolean deriveFromTransferFiles = task instanceof AbstractIli2DbFileTask fileTask
                && fileTask.getDatasetNamesFromTransferFiles().get();
        boolean deriveFromFiles = task instanceof AbstractIli2DbFileTask fileTask
                && !fileTask.getDatasetNameFilesCollection().isEmpty();

        int strategies = (explicitNames.isEmpty() ? 0 : 1)
                + (hasLegacyDataset ? 1 : 0)
                + (deriveFromTransferFiles ? 1 : 0)
                + (deriveFromFiles ? 1 : 0);

        if (strategies == 0) {
            return List.of();
        }
        if (strategies > 1) {
            throw new GradleException("Use only one dataset naming strategy.");
        }

        List<String> names;
        if (!explicitNames.isEmpty()) {
            ensureNoLegacySubstring(task);
            names = explicitNames;
        } else if (hasLegacyDataset) {
            names = resolveLegacyDataset(task, legacyDataset);
        } else {
            AbstractIli2DbFileTask fileTask = (AbstractIli2DbFileTask) task;
            if (inputs == null || !inputs.usesLocalFiles()) {
                throw new GradleException("Derived dataset names require local transfer files.");
            }
            List<File> sourceFiles = deriveFromTransferFiles
                    ? fileTask.getTransferFilesCollection().getFiles().stream()
                    .sorted(Comparator.comparing(File::getPath))
                    .toList()
                    : fileTask.getDatasetNameFilesCollection().getFiles().stream()
                    .sorted(Comparator.comparing(File::getPath))
                    .toList();
            if (sourceFiles.isEmpty()) {
                throw new GradleException("No files available to derive dataset names from.");
            }
            names = sourceFiles.stream()
                    .map(File::getName)
                    .map(DatasetNameResolver::stripExtension)
                    .map(name -> applyDatasetNameSlice(name, fileTask))
                    .toList();
        }

        if (inputs != null && !names.isEmpty()) {
            validateCount(inputs.executionInputs().size(), names.size());
        }
        return names;
    }

    private List<String> resolveLegacyDataset(AbstractIli2DbTask task, Object source) {
        if (source instanceof String value) {
            return List.of(value);
        }
        if (source instanceof FileCollection files) {
            return files.getFiles().stream()
                    .sorted(Comparator.comparing(File::getPath))
                    .map(File::getName)
                    .map(DatasetNameResolver::stripExtension)
                    .map(value -> applyLegacySubstring(value, task.getDatasetSubstring().get()))
                    .toList();
        }
        if (source instanceof Iterable<?> iterable) {
            List<String> values = new ArrayList<>();
            for (Object item : iterable) {
                if (item == null) {
                    throw new GradleException("dataset entries must not be null");
                }
                values.add(applyLegacySubstring(String.valueOf(item), task.getDatasetSubstring().get()));
            }
            return List.copyOf(values);
        }
        throw new GradleException("dataset: illegal data type <" + source.getClass() + ">");
    }

    private void ensureNoLegacySubstring(AbstractIli2DbTask task) {
        if (!task.getDatasetSubstring().get().isEmpty()) {
            throw new GradleException("datasetSubstring can only be used with dataset(...) aliases.");
        }
    }

    private static String stripExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
    }

    private static String applyLegacySubstring(String value, List<Integer> substring) {
        if (substring == null || substring.isEmpty()) {
            return value;
        }
        int start = substring.get(0);
        int endExclusive = substring.size() > 1 ? substring.get(substring.size() - 1) : value.length();
        if (start < 0 || start > value.length() || endExclusive < start || endExclusive > value.length()) {
            throw new GradleException("datasetSubstring " + substring + " is outside dataset value '" + value + "'");
        }
        return value.substring(start, endExclusive);
    }

    private static String applyDatasetNameSlice(String value, AbstractIli2DbFileTask task) {
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

    private static void validateCount(int fileCount, int datasetCount) {
        if (fileCount != datasetCount) {
            throw new GradleException("number of dataset names (" + datasetCount + ") doesn't match number of files (" + fileCount + ")");
        }
    }
}
