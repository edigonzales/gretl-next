package ch.so.agi.gretl.geotools.internal.operations;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record RasterReclassifyRequest(
        String taskName,
        Path inputRaster,
        Path outputRaster,
        double noData,
        List<Double> breaks
) implements GeoToolsOperationRequest {

    @Override
    public String operation() {
        return "raster-reclassify";
    }

    @Override
    public Map<String, String> parameters() {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("taskName", taskName);
        parameters.put("inputRaster", inputRaster.toAbsolutePath().toString());
        parameters.put("outputRaster", outputRaster.toAbsolutePath().toString());
        parameters.put("noData", String.valueOf(noData));
        return parameters;
    }

    @Override
    public List<Double> values() {
        return List.copyOf(breaks);
    }
}
