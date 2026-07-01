package ch.so.agi.gretl.lsp.metadata;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public final class MetadataLoader {

    private static final String DEFAULT_RESOURCE_PATH = "ch/so/agi/gretl/lsp/metadata/gretl-lsp-metadata.json";
    private static final String EXPECTED_SCHEMA_VERSION = "1.0.0";

    private final ObjectMapper objectMapper;

    public MetadataLoader() {
        this.objectMapper = JsonMapper.builder()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .build();
    }

    public GretlMetadata loadDefault() {
        URL resource = getClass().getClassLoader().getResource(DEFAULT_RESOURCE_PATH);
        if (resource == null) {
            return GretlMetadata.empty();
        }
        try (InputStream in = resource.openStream()) {
            return load(in);
        } catch (IOException e) {
            return GretlMetadata.empty();
        }
    }

    public GretlMetadata load(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path)) {
            return load(in);
        }
    }

    public GretlMetadata load(InputStream inputStream) throws IOException {
        GretlMetadata metadata = objectMapper.readValue(inputStream, GretlMetadata.class);
        if (metadata.schemaVersion() != null && !metadata.schemaVersion().equals(EXPECTED_SCHEMA_VERSION)) {
            return GretlMetadata.empty();
        }
        return metadata;
    }
}
