package ch.so.agi.gretl.internal.gzip;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

public class GzipEngine {

    public void execute(Path inputFile, Path gzipFile) throws IOException {
        Path parent = gzipFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (InputStream input = Files.newInputStream(inputFile);
             OutputStream output = Files.newOutputStream(gzipFile);
             GZIPOutputStream gzip = new GZIPOutputStream(output)) {
            input.transferTo(gzip);
        }
    }
}
