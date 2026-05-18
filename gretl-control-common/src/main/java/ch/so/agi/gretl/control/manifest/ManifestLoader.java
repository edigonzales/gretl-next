package ch.so.agi.gretl.control.manifest;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ManifestLoader {
    private final ObjectMapper mapper;
    private final ManifestValidator validator;

    public ManifestLoader() {
        this(new ManifestValidator());
    }

    public ManifestLoader(ManifestValidator validator) {
        this.mapper = JsonMapper.builder(new YAMLFactory())
                .propertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .build();
        this.validator = validator;
    }

    public GretlServerManifest load(Path manifestPath) {
        if (!Files.exists(manifestPath)) {
            throw new ManifestException("GRETL server manifest not found: " + manifestPath.toAbsolutePath());
        }
        try {
            GretlServerManifest manifest = mapper.readValue(manifestPath.toFile(), GretlServerManifest.class);
            validator.validate(manifest);
            return manifest;
        } catch (IOException e) {
            throw new ManifestException("Could not read GRETL server manifest: " + manifestPath.toAbsolutePath(), e);
        }
    }
}
