package ch.so.agi.gretl.internal.db2db;

import ch.so.agi.gretl.util.GretlException;
import org.gradle.api.GradleException;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GeometryTransformTest {

    @Test
    void createsWktWkbAndGeoJsonValueExpressions() {
        DbTransferSpec spec = new DbTransferSpec(
                Path.of("select.sql"),
                "parcels",
                false,
                List.of("geom_wkt:WKT:2056", "geom_wkb:WKB:2056", "geom_geojson:GEOJSON:4326")
        );

        assertEquals("ST_GeomFromText(?, 2056)", spec.valueExpression("geom_wkt"));
        assertEquals("ST_GeomFromWKB(?, 2056)", spec.valueExpression("geom_wkb"));
        assertEquals("ST_SetSRID(ST_GeomFromGeoJSON(?), 4326)", spec.valueExpression("geom_geojson"));
        assertEquals("?", spec.valueExpression("name"));
    }

    @Test
    void rejectsMalformedGeometryDefinition() {
        assertThrows(GretlException.class, () -> GeometryTransform.create("geom"));
    }

    @Test
    void rejectsUnknownGeometryStandard() {
        assertThrows(GretlException.class, () -> GeometryTransform.create("geom:EWKB:2056"));
    }

    @Test
    void rejectsMissingEpsgCode() {
        assertThrows(GradleException.class, () -> GeometryTransform.create("geom:WKT"));
    }

    @Test
    void rejectsNonNumericEpsgCode() {
        assertThrows(GretlException.class, () -> GeometryTransform.create("geom:WKB:abc"));
    }
}
