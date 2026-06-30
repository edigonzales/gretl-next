package ch.so.agi.gretl.internal.interlis;

import ch.ehi.basics.settings.Settings;
import ch.interlis.ili2c.metamodel.TransferDescription;
import ch.interlis.iox.IoxException;
import ch.interlis.iox.IoxReader;
import ch.interlis.iox_j.PipelinePool;
import ch.interlis.iox_j.logging.LogEventFactory;
import ch.interlis.ioxwkf.json.JsonReader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.interlis2.validator.Validator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class JsonValidatorImpl extends Validator {

    @Override
    protected IoxReader createReader(String filename, TransferDescription td, LogEventFactory errFactory,
                                     Settings settings, PipelinePool pool) throws IoxException {
        try {
            JsonReader reader = new JsonReader(preprocessJsonFile(filename).toFile(), settings);
            reader.setModel(td);
            return reader;
        } catch (IOException e) {
            throw new IoxException(e);
        }
    }

    private Path preprocessJsonFile(String jsonFile) throws IOException {
        Path tempDir = Files.createTempDirectory("jsonvalidator_");
        Path target = tempDir.resolve(Path.of(jsonFile).getFileName());

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(new File(jsonFile));

        if (!rootNode.isArray()) {
            if (rootNode.isObject()) {
                addAttributes((ObjectNode) rootNode, 1);
            }
            ArrayNode array = objectMapper.createArrayNode();
            array.add(rootNode);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(target.toFile(), array);
        } else {
            int idCounter = 1;
            for (JsonNode node : rootNode) {
                if (node.isObject()) {
                    addAttributes((ObjectNode) node, idCounter++);
                }
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(target.toFile(), rootNode);
        }
        return target;
    }

    private void addAttributes(ObjectNode objectNode, int id) {
        JsonNode typeNode = objectNode.get("@type");
        if (typeNode == null || !typeNode.isTextual()) {
            throw new IllegalArgumentException("Missing or invalid @type attribute in JSON object.");
        }

        if (!objectNode.has("@topic")) {
            String[] parts = typeNode.asText().split("\\.");
            if (parts.length >= 2) {
                objectNode.put("@topic", parts[0] + "." + parts[1]);
            }
        }
        if (!objectNode.has("@id")) {
            objectNode.put("@id", "o" + id);
        }
        if (!objectNode.has("@bid")) {
            objectNode.put("@bid", "b1");
        }
    }
}
