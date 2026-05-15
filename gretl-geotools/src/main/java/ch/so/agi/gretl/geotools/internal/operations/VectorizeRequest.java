package ch.so.agi.gretl.geotools.internal.operations;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record VectorizeRequest(
        String taskName,
        Path inputRaster,
        Path outputGeopackage,
        int band,
        List<Double> cellValues
) implements GeoToolsOperationRequest {

    @Override
    public String operation() {
        return "vectorize";
    }

    @Override
    public Map<String, String> parameters() {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("taskName", taskName);
        parameters.put("inputRaster", inputRaster.toAbsolutePath().toString());
        parameters.put("outputGeopackage", outputGeopackage.toAbsolutePath().toString());
        parameters.put("band", String.valueOf(band));
        return parameters;
    }

    @Override
    public List<Double> values() {
        return List.copyOf(cellValues);
    }
}
