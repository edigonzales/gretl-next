package ch.so.agi.gretl.geotools.worker.operations;

import org.eclipse.imagen.media.range.NoDataContainer;
import org.geotools.api.parameter.GeneralParameterValue;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.NoSuchAuthorityCodeException;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.coverage.util.CoverageUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RasterReclassifyEngineTest {

    private static final double DEFAULT_NO_DATA = -100d;

    @TempDir
    Path tempDir;

    @Test
    void reclassifyProducesOnlyConfiguredClassValues()
            throws IOException, NoSuchAuthorityCodeException, FactoryException {
        Path input = Path.of("src/test/resources/fixtures/raster-reclassify/Beispiel_Rasterfile.asc");
        Path output = tempDir.resolve("reclass.tif");

        new RasterReclassifyEngine("test").execute(input, output);

        GridCoverage2D coverage = readCoverage(output);
        Set<Double> values = readClassValues(coverage);
        Set<Double> allowed = Set.of(0d, 55d, 60d, 65d, 70d, DEFAULT_NO_DATA);

        values.forEach(value -> assertTrue(allowed.contains(value), "Unexpected class value: " + value));
        assertTrue(values.stream().anyMatch(value -> value != DEFAULT_NO_DATA));

        NoDataContainer noData = CoverageUtilities.getNoDataProperty(coverage);
        assertNotNull(noData);
        assertEquals(DEFAULT_NO_DATA, noData.getAsSingleValue());
    }

    @Test
    void customBreaksAndNoDataAreApplied()
            throws IOException, NoSuchAuthorityCodeException, FactoryException {
        Path input = Path.of("src/test/resources/fixtures/raster-reclassify/Beispiel_Rasterfile.asc");
        Path output = tempDir.resolve("custom-reclass.tif");

        new RasterReclassifyEngine("test").execute(input, output, new double[] {0, 40, 42, 45}, -5d);

        GridCoverage2D coverage = readCoverage(output);
        Set<Double> values = readClassValues(coverage);
        Set<Double> expected = Set.of(0d, 40d, 42d, -5d);

        assertEquals(expected, values);

        NoDataContainer noData = CoverageUtilities.getNoDataProperty(coverage);
        assertNotNull(noData);
        assertEquals(-5d, noData.getAsSingleValue());
    }

    private GridCoverage2D readCoverage(Path rasterPath) throws IOException {
        AbstractGridFormat format = GridFormatFinder.findFormat(rasterPath.toFile());
        GridCoverage2DReader reader = null;
        try {
            reader = format.getReader(rasterPath.toFile());
            return reader.read((GeneralParameterValue[]) null);
        } finally {
            if (reader != null) {
                reader.dispose();
            }
        }
    }

    private Set<Double> readClassValues(GridCoverage2D coverage) {
        RenderedImage image = coverage.getRenderedImage();
        Raster raster = image.getData();
        Set<Double> values = new HashSet<>();
        int width = raster.getWidth();
        int height = raster.getHeight();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                values.add(raster.getSampleDouble(x, y, 0));
            }
        }
        return values;
    }
}
