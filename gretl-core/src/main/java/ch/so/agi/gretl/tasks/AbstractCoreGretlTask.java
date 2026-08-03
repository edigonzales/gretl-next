package ch.so.agi.gretl.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Provider;

import java.io.File;
import java.nio.file.Path;

public abstract class AbstractCoreGretlTask extends DefaultTask {

    protected final void setRegularFile(RegularFileProperty property, Object path) {
        requirePath(path, "regular file");
        if (path instanceof Provider<?> provider) {
            property.set(provider.map(value -> toRegularFile(value, "regular file provider")));
        } else {
            property.set(toRegularFile(path, "regular file"));
        }
    }

    protected final void setDirectory(DirectoryProperty property, Object path) {
        requirePath(path, "directory");
        if (path instanceof Provider<?> provider) {
            property.set(provider.map(value -> toDirectory(value, "directory provider")));
        } else {
            property.set(toDirectory(path, "directory"));
        }
    }

    private void requirePath(Object path, String description) {
        if (path == null) {
            throw new IllegalArgumentException(description + " path must not be null");
        }
    }

    private RegularFile toRegularFile(Object value, String description) {
        requirePath(value, description);
        if (value instanceof RegularFile regularFile) {
            return regularFile;
        }
        if (value instanceof Directory) {
            throw new IllegalArgumentException(description + " must resolve to a file, not a directory");
        }
        File file = toFile(value);
        return getProject().getLayout().file(getProject().provider(() -> file)).get();
    }

    private Directory toDirectory(Object value, String description) {
        requirePath(value, description);
        if (value instanceof Directory directory) {
            return directory;
        }
        if (value instanceof RegularFile) {
            throw new IllegalArgumentException(description + " must resolve to a directory, not a file");
        }
        File file = toFile(value);
        return getProject().getLayout().dir(getProject().provider(() -> file)).get();
    }

    private File toFile(Object value) {
        if (value instanceof File file) {
            return file;
        }
        if (value instanceof Path path) {
            return path.toFile();
        }
        return getProject().file(value);
    }
}
