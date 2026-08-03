package ch.so.agi.gretl.job.assertions;

import ch.so.agi.gretl.test.job.TestJobVerificationContext;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.zip.GZIPInputStream;
import org.geotools.api.parameter.GeneralParameterValue;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.referencing.CRS;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class CombinedRasterTestJobAssertions extends AbstractCanonicalTestJobAssertions {
    @Override public String id() { return "combined-core-geotools-pipeline"; }
    @Override protected void verifyJob(TestJobVerificationContext context) throws Exception {
        var job = context.job();
        var ascii = job.resolve("build/generated/raster.asc");
        var geotiff = job.resolve("build/geotools/reclassified.tif");
        var gzip = job.resolve("build/distribution/reclassified.tif.gz");
        assertTrue(Files.isRegularFile(ascii)); assertTrue(Files.isRegularFile(geotiff)); assertTrue(Files.isRegularFile(gzip));
        try (var input = new GZIPInputStream(Files.newInputStream(gzip))) {
            assertArrayEquals(Files.readAllBytes(geotiff), input.readAllBytes());
        }
        List<String> lines = Files.readAllLines(ascii, StandardCharsets.UTF_8);
        assertEquals("ncols 4", lines.get(0).trim()); assertEquals("nrows 3", lines.get(1).trim());
        assertEquals("10 56 61 71", lines.get(6).trim()); assertEquals("-9999 0 70 500", lines.get(8).trim());
        AbstractGridFormat format = GridFormatFinder.findFormat(geotiff.toFile()); assertNotNull(format);
        GridCoverage2DReader reader = format.getReader(geotiff.toFile());
        try {
            GridCoverage2D coverage = reader.read((GeneralParameterValue[]) null);
            assertEquals(4, coverage.getRenderedImage().getWidth()); assertEquals(3, coverage.getRenderedImage().getHeight());
            assertEquals("EPSG:2056", CRS.lookupIdentifier(coverage.getCoordinateReferenceSystem2D(), true));
            double[][] expected = {{0,55,60,70},{0,55,60,70},{-100,0,70,70}};
            var raster = coverage.getRenderedImage().getData();
            for (int row=0; row<expected.length; row++) for (int col=0; col<expected[row].length; col++)
                assertEquals(expected[row][col], raster.getSampleDouble(col, row, 0), 0.000001);
        } finally { reader.dispose(); }
    }
}
