package ch.so.agi.gretl.control.server.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class JsonSupport {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper mapper;

    public JsonSupport(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public String writeMap(Map<String, Object> value) {
        try {
            return mapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not serialize map.", e);
        }
    }

    public Map<String, Object> readMap(String value) {
        if (value == null || value.isBlank()) {
            return Map.of();
        }
        try {
            return mapper.readValue(value, MAP_TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not deserialize map.", e);
        }
    }

    public String writeStringList(List<String> value) {
        try {
            return mapper.writeValueAsString(value == null ? List.of() : value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not serialize string list.", e);
        }
    }

    public List<String> readStringList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            return mapper.readValue(value, STRING_LIST_TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not deserialize string list.", e);
        }
    }
}
