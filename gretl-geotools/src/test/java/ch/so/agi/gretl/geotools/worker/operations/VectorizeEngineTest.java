package ch.so.agi.gretl.geotools.worker.operations;

import org.geotools.api.data.SimpleFeatureReader;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.filter.Filter;
import org.geotools.api.metadata.spatial.PixelOrientation;
import org.geotools.api.parameter.GeneralParameterValue;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.geopkg.FeatureEntry;
import org.geotools.geopkg.GeoPackage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.locationtech.jts.geom.MultiPolygon;

import java.awt.geom.AffineTransform;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VectorizeEngineTest {

    @TempDir
    Path tempDir;

    @Test
    void vectorizesRasterCellsIntoMultipolygon() throws Exception {
        Path raster = Path.of("src/test/resources/original-gretl-gt/vectorize/reclass.tif");
        Path geopackage = tempDir.resolve("vectorized.gpkg");
        double cellValue = 55d;

        new VectorizeEngine("test").execute(raster, geopackage, 0, List.of(cellValue));

        assertTrue(geopackage.toFile().exists(), "Vectorize engine must create the GeoPackage file");

        double expectedArea = calculateExpectedArea(raster, 0, cellValue);

        try (GeoPackage gpkg = new GeoPackage(geopackage.toFile())) {
            gpkg.init();
            FeatureEntry entry = gpkg.feature("reclass");
            assertNotNull(entry, "Expected GeoPackage layer named after the raster");

            try (SimpleFeatureReader reader = gpkg.reader(entry, Filter.INCLUDE, null)) {
                assertTrue(reader.hasNext(), "Result layer must contain a feature");
                SimpleFeature feature = reader.next();
                assertTrue(feature.getDefaultGeometry() instanceof MultiPolygon);
                MultiPolygon geometry = (MultiPolygon) feature.getDefaultGeometry();
                assertFalse(geometry.isEmpty());
                assertEquals(cellValue, ((Number) feature.getAttribute("value")).doubleValue(), 1e-9);

                double tolerance = Math.max(1e-6, expectedArea * 1e-6);
                assertEquals(expectedArea, geometry.getArea(), tolerance);
                assertFalse(reader.hasNext(), "Only one dissolved feature is expected");
            }
        }
    }

    @Test
    void vectorizesMultipleCellValuesIntoSeparateFeatures() throws Exception {
        Path raster = Path.of("src/test/resources/original-gretl-gt/vectorize/reclass.tif");
        Path geopackage = tempDir.resolve("vectorized-multi.gpkg");
        List<Double> cellValues = List.of(55d, 60d, 99d);

        Map<Double, Double> expectedAreas = new HashMap<>();
        for (double value : cellValues) {
            expectedAreas.put(value, calculateExpectedArea(raster, 0, value));
        }

        new VectorizeEngine("test").execute(raster, geopackage, 0, cellValues);

        try (GeoPackage gpkg = new GeoPackage(geopackage.toFile())) {
            gpkg.init();
            FeatureEntry entry = gpkg.feature("reclass");
            assertNotNull(entry, "Expected GeoPackage layer named after the raster");

            Map<Double, MultiPolygon> geometriesByValue = new HashMap<>();
            try (SimpleFeatureReader reader = gpkg.reader(entry, Filter.INCLUDE, null)) {
                while (reader.hasNext()) {
                    SimpleFeature feature = reader.next();
                    double value = ((Number) feature.getAttribute("value")).doubleValue();
                    assertTrue(feature.getDefaultGeometry() instanceof MultiPolygon);
                    geometriesByValue.put(value, (MultiPolygon) feature.getDefaultGeometry());
                }
            }

            long expectedFeatureCount = expectedAreas.values().stream().filter(area -> area > 0).count();
            assertEquals(expectedFeatureCount, geometriesByValue.size());

            for (Map.Entry<Double, Double> expected : expectedAreas.entrySet()) {
                double value = expected.getKey();
                double expectedArea = expected.getValue();
                if (expectedArea > 0) {
                    assertTrue(geometriesByValue.containsKey(value), "Expected feature for value " + value);
                    double tolerance = Math.max(1e-6, expectedArea * 1e-6);
                    assertEquals(expectedArea, geometriesByValue.get(value).getArea(), tolerance);
                } else {
                    assertFalse(geometriesByValue.containsKey(value), "No feature expected for absent value " + value);
                }
            }
        }
    }

    private double calculateExpectedArea(Path rasterPath, int band, double targetValue) throws IOException {
        File rasterFile = rasterPath.toFile();
        AbstractGridFormat format = GridFormatFinder.findFormat(rasterFile);
        GridCoverage2DReader reader = null;
        try {
            reader = format.getReader(rasterFile);
            GridCoverage2D coverage = reader.read((GeneralParameterValue[]) null);
            AffineTransform transform =
                    (AffineTransform) coverage.getGridGeometry().getGridToCRS2D(PixelOrientation.UPPER_LEFT);
            double determinant = transform.getScaleX() * transform.getScaleY()
                    - transform.getShearX() * transform.getShearY();
            double cellArea = Math.abs(determinant);

            Raster raster = coverage.getRenderedImage().getData();
            int minX = raster.getMinX();
            int minY = raster.getMinY();
            int maxX = minX + raster.getWidth();
            int maxY = minY + raster.getHeight();
            int matches = 0;
            for (int y = minY; y < maxY; y++) {
                for (int x = minX; x < maxX; x++) {
                    double sample = raster.getSampleDouble(x, y, band);
                    if (Math.abs(sample - targetValue) < 1e-6) {
                        matches++;
                    }
                }
            }
            return matches * cellArea;
        } finally {
            if (reader != null) {
                reader.dispose();
            }
        }
    }
}
