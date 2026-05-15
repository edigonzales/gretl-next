package ch.so.agi.gretl.internal.db2db;

import ch.so.agi.gretl.util.GretlException;
import org.gradle.api.GradleException;

public abstract class GeometryTransform {

    private static final String WKB = "WKB";
    private static final String WKT = "WKT";
    private static final String GEOJSON = "GEOJSON";

    private final String columnNameUpperCase;

    GeometryTransform(String columnName) {
        this.columnNameUpperCase = columnName.toUpperCase();
    }

    static GeometryTransform create(String columnDefinition) {
        if (columnDefinition == null) {
            throw new GretlException("Column definition must not be null");
        }

        String[] parts = columnDefinition.split(":");
        if (parts.length < 2) {
            throw new GretlException(
                    "Malformed geometry column definition. Expecting [colname]:[geomtype]:[epsg].");
        }

        String geometryType = parts[1].toUpperCase();
        if (WKB.equals(geometryType)) {
            return new WkbGeometryTransform(parts);
        }
        if (WKT.equals(geometryType)) {
            return new WktGeometryTransform(parts);
        }
        if (GEOJSON.equals(geometryType)) {
            return new GeoJsonGeometryTransform(parts);
        }
        throw new GretlException("Unknown geometry standard. Expecting WKB, WKT or GEOJSON");
    }

    String columnNameUpperCase() {
        return columnNameUpperCase;
    }

    abstract String wrap(String valuePlaceholder);

    abstract String formatInfo();

    static int parseEpsgCode(String epsgCodeString) {
        try {
            return Integer.parseInt(epsgCodeString);
        } catch (NumberFormatException e) {
            throw new GretlException(
                    String.format("Given epsg code [%s] in column configuration is not a number", epsgCodeString),
                    e);
        }
    }

    private static final class WkbGeometryTransform extends GeometryTransform {
        private final int epsgCode;

        private WkbGeometryTransform(String[] parts) {
            super(parts[0]);
            if (parts.length != 3) {
                throw new GradleException(
                        String.format("Configuration error. Expecting format string %s for wkb", formatInfo()));
            }
            this.epsgCode = parseEpsgCode(parts[2]);
        }

        @Override
        String wrap(String valuePlaceholder) {
            return String.format("ST_GeomFromWKB(%s, %s)", valuePlaceholder, epsgCode);
        }

        @Override
        String formatInfo() {
            return "[colname]:WKB:[epsg_code]. All case insensitive.";
        }
    }

    private static final class WktGeometryTransform extends GeometryTransform {
        private final int epsgCode;

        private WktGeometryTransform(String[] parts) {
            super(parts[0]);
            if (parts.length != 3) {
                throw new GradleException(
                        String.format("Configuration error. Expecting format string %s for wkt", formatInfo()));
            }
            this.epsgCode = parseEpsgCode(parts[2]);
        }

        @Override
        String wrap(String valuePlaceholder) {
            return String.format("ST_GeomFromText(%s, %s)", valuePlaceholder, epsgCode);
        }

        @Override
        String formatInfo() {
            return "[colname]:WKT:[epsg_code]. All case insensitive.";
        }
    }

    private static final class GeoJsonGeometryTransform extends GeometryTransform {
        private final int epsgCode;

        private GeoJsonGeometryTransform(String[] parts) {
            super(parts[0]);
            if (parts.length != 3) {
                throw new GradleException(
                        String.format("Configuration error. Expecting format string %s for geojson", formatInfo()));
            }
            this.epsgCode = parseEpsgCode(parts[2]);
        }

        @Override
        String wrap(String valuePlaceholder) {
            return String.format("ST_SetSRID(ST_GeomFromGeoJSON(%s), %s)", valuePlaceholder, epsgCode);
        }

        @Override
        String formatInfo() {
            return "[columnName]:GEOJSON:[epsg_code]. All case insensitive.";
        }
    }
}
