package ch.so.agi.gretl.job.assertions;

import ch.so.agi.gretl.test.job.TestJobVerificationContext;
import java.nio.file.Files;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.locationtech.jts.geom.Geometry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ReadShapefileTestJobAssertions extends AbstractCanonicalTestJobAssertions {
    @Override public String id() { return "geotools-read-shapefile"; }
    @Override protected void verifyJob(TestJobVerificationContext context) throws Exception {
        String expected = Files.readString(context.job().resolveExpected("features.json"));
        var minimumFeatureCount = java.util.regex.Pattern.compile("\\\"minimumFeatureCount\\\"\\s*:\\s*(\\d+)")
                .matcher(expected);
        assertTrue(minimumFeatureCount.find(), expected);
        var shapefile = context.job().resolve("input/points.shp");
        DataStore store = DataStoreFinder.getDataStore(java.util.Map.of("url", shapefile.toUri().toURL()));
        try {
            assertNotNull(store);
            String typeName = store.getTypeNames()[0];
            SimpleFeatureType schema = store.getSchema(typeName);
            var features = store.getFeatureSource(typeName).getFeatures();
            int count = features.size();
            assertTrue(count >= Integer.parseInt(minimumFeatureCount.group(1)),
                    "Feature count below expected minimum: " + count);
            assertTrue(count > 0);
            assertNotNull(schema.getCoordinateReferenceSystem());
            assertTrue(schema.getDescriptor("t_id") != null);
            assertTrue(schema.getDescriptor("aint") != null);
            try (var iterator = features.features()) {
                assertTrue(iterator.hasNext());
                SimpleFeature feature = iterator.next();
                assertNotNull(feature.getDefaultGeometry());
                assertEquals("Point", ((Geometry) feature.getDefaultGeometry()).getGeometryType());
                assertNotNull(feature.getAttribute("t_id"));
            }
        } finally {
            if (store != null) store.dispose();
        }
        assertTrue(context.result().output().contains("Feature count:"), context.result().output());
        assertTrue(context.result().output().contains("Target CRS: EPSG:2056"), context.result().output());
    }
}
