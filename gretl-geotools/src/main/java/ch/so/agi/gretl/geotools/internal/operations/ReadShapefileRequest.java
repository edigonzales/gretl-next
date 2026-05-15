package ch.so.agi.gretl.geotools.internal.operations;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public record ReadShapefileRequest(String taskName, Path shapefile, String crsCode)
        implements GeoToolsOperationRequest {

    @Override
    public String operation() {
        return "read-shapefile";
    }

    @Override
    public Map<String, String> parameters() {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("taskName", taskName);
        parameters.put("shapefile", shapefile.toAbsolutePath().toString());
        if (crsCode != null && !crsCode.isBlank()) {
            parameters.put("crsCode", crsCode);
        }
        return parameters;
    }
}
