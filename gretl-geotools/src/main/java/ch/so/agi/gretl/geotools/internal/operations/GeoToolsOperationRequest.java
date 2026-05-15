package ch.so.agi.gretl.geotools.internal.operations;

import java.util.List;
import java.util.Map;

public interface GeoToolsOperationRequest {

    String operation();

    Map<String, String> parameters();

    default List<Double> values() {
        return List.of();
    }
}
