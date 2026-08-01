package ch.so.agi.gretl.combined.assertions;

import org.geotools.api.parameter.GeneralParameterValue;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;

import java.awt.image.Raster;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class GeoTiffAssertions {
    public record RasterSummary(int width, int height, int bands, String crsCode, double noData,
                                double[][] values, ReferencedEnvelope envelope) {
    }

    public static RasterSummary read(Path path) {
        AbstractGridFormat format = GridFormatFinder.findFormat(path.toFile());
        if (format == null) {
            throw new AssertionError("No GeoTools raster format found for " + path);
        }
        GridCoverage2DReader reader = null;
        try {
            reader = format.getReader(path.toFile());
            GridCoverage2D coverage = reader.read((GeneralParameterValue[]) null);
            Raster raster = coverage.getRenderedImage().getData();
            double[][] values = new double[raster.getHeight()][raster.getWidth()];
            for (int row = 0; row < raster.getHeight(); row++) {
                for (int column = 0; column < raster.getWidth(); column++) {
                    values[row][column] = raster.getSampleDouble(column, row, 0);
                }
            }
            CoordinateReferenceSystem crs = coverage.getCoordinateReferenceSystem2D();
            String code = crs == null ? "" : String.valueOf(CRS.lookupIdentifier(crs, true));
            return new RasterSummary(raster.getWidth(), raster.getHeight(), raster.getNumBands(),
                    code, noData(coverage), values, coverage.getEnvelope2D());
        } catch (Exception e) {
            throw new AssertionError("Cannot read GeoTIFF " + path, e);
        } finally {
            if (reader != null) {
                try {
                    reader.dispose();
                } catch (IOException e) {
                    throw new AssertionError("Cannot dispose GeoTools reader for " + path, e);
                }
            }
        }
    }

    public static void assertDimensions(RasterSummary summary, int width, int height) {
        assertEquals(width, summary.width());
        assertEquals(height, summary.height());
    }

    public static void assertCrs(RasterSummary summary, String expectedCode) {
        assertEquals(expectedCode.replace("EPSG:", ""), summary.crsCode().replace("EPSG:", ""));
    }

    public static void assertNoData(RasterSummary summary, double expected) {
        assertEquals(expected, summary.noData(), 0.000001);
    }

    public static void assertBandValues(RasterSummary summary, int band, double[][] expected) {
        assertEquals(0, band, "The combined raster canary has one band at index 0");
        assertEquals(expected.length, summary.values().length);
        for (int row = 0; row < expected.length; row++) {
            assertArrayEquals(expected[row], summary.values()[row], 0.000001,
                    "Unexpected GeoTIFF values in row " + row);
        }
    }

    public static void assertEnvelope(RasterSummary summary, double minX, double minY,
                                      double maxX, double maxY) {
        assertEquals(minX, summary.envelope().getMinX(), 0.000001);
        assertEquals(minY, summary.envelope().getMinY(), 0.000001);
        assertEquals(maxX, summary.envelope().getMaxX(), 0.000001);
        assertEquals(maxY, summary.envelope().getMaxY(), 0.000001);
    }

    private static double noData(GridCoverage2D coverage) {
        try {
            Object dimension = coverage.getSampleDimension(0);
            Method method = dimension.getClass().getMethod("getNoDataValues");
            double[] values = (double[]) method.invoke(dimension);
            if (values != null && values.length > 0) {
                return values[0];
            }
        } catch (ReflectiveOperationException ignored) {
            // Some GeoTools readers expose no-data only through the raster sample values.
        }
        return Double.NaN;
    }

    private GeoTiffAssertions() {
    }
}
