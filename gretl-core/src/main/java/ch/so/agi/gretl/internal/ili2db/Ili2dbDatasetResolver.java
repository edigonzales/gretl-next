package ch.so.agi.gretl.internal.ili2db;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class Ili2dbDatasetResolver {

    private Ili2dbDatasetResolver() {
    }

    public static ResolvedDataFiles resolveDataFiles(Project project, Object source) {
        if (source == null) {
            return new ResolvedDataFiles(List.of(), List.of(), List.of());
        }
        if (source instanceof String value && value.startsWith("ilidata:")) {
            return new ResolvedDataFiles(List.of(value), List.of(), List.of(value));
        }
        FileCollection files = source instanceof FileCollection fileCollection
                ? fileCollection
                : project.files(source);
        if (!(source instanceof FileCollection) && source instanceof Iterable<?> iterable) {
            List<String> identifiers = new ArrayList<>();
            for (Object item : iterable) {
                if (!(item instanceof String value) || !value.startsWith("ilidata:")) {
                    throw new GradleException("dataFile list entries must be ilidata: IDs. Use files(...) or fileTree(...) for local files.");
                }
                identifiers.add(value);
            }
            return new ResolvedDataFiles(List.copyOf(identifiers), List.of(), List.copyOf(identifiers));
        }
        List<Path> localFiles = sortedFiles(files).stream().map(File::toPath).toList();
        return new ResolvedDataFiles(localFiles.stream().map(Path::toString).toList(), localFiles, List.of());
    }

    public static ResolvedOutputFiles resolveOutputFiles(Project project, Object source) {
        if (source == null) {
            return new ResolvedOutputFiles(List.of(), List.of());
        }
        FileCollection files = source instanceof FileCollection fileCollection
                ? fileCollection
                : project.files(source);
        List<Path> outputFiles = sortedFiles(files).stream().map(File::toPath).toList();
        return new ResolvedOutputFiles(outputFiles.stream().map(Path::toString).toList(), outputFiles);
    }

    public static List<String> resolveDatasets(Object source, List<Integer> substring) {
        if (source == null) {
            return List.of();
        }
        if (source instanceof String value) {
            return List.of(value);
        }
        if (source instanceof FileCollection files) {
            return sortedFiles(files).stream()
                    .map(File::getName)
                    .map(Ili2dbDatasetResolver::removeExtension)
                    .map(value -> substring(value, substring))
                    .toList();
        }
        if (source instanceof Iterable<?> iterable) {
            List<String> values = new ArrayList<>();
            for (Object item : iterable) {
                if (item == null) {
                    throw new GradleException("dataset entries must not be null");
                }
                values.add(substring(String.valueOf(item), substring));
            }
            return List.copyOf(values);
        }
        throw new GradleException("dataset: illegal data type <" + source.getClass() + ">");
    }

    public static List<Ili2dbTransfer> pairFilesAndDatasets(List<String> files, List<String> datasets) {
        if (files.isEmpty()) {
            return List.of();
        }
        if (!datasets.isEmpty() && files.size() != datasets.size()) {
            throw new GradleException("number of dataset names (" + datasets.size()
                    + ") doesn't match number of files (" + files.size() + ")");
        }
        List<Ili2dbTransfer> transfers = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            transfers.add(new Ili2dbTransfer(files.get(i), datasets.isEmpty() ? null : datasets.get(i)));
        }
        return List.copyOf(transfers);
    }

    private static List<File> sortedFiles(FileCollection files) {
        return files.getFiles().stream()
                .sorted(Comparator.comparing(File::getPath))
                .toList();
    }

    private static String removeExtension(String value) {
        int dot = value.lastIndexOf('.');
        return dot < 0 ? value : value.substring(0, dot);
    }

    private static String substring(String value, List<Integer> substring) {
        if (substring == null || substring.isEmpty()) {
            return value;
        }
        int from = substring.get(0);
        int to = substring.size() > 1 ? substring.get(substring.size() - 1) : value.length();
        if (from < 0 || to < from || to > value.length()) {
            throw new GradleException("datasetSubstring " + substring + " is outside dataset value '" + value + "'");
        }
        return value.substring(from, to);
    }

    public record ResolvedDataFiles(List<String> files, List<Path> localFiles, List<String> identifiers) {
    }

    public record ResolvedOutputFiles(List<String> files, List<Path> outputFiles) {
    }
}
