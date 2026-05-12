package ch.so.agi.gretl.geotools.worker;

import ch.so.agi.gretl.geotools.worker.logging.GretlLogger;
import ch.so.agi.gretl.geotools.worker.logging.LogEnvironment;
import ch.so.agi.gretl.geotools.worker.steps.RasterReclassifyStep;
import ch.so.agi.gretl.geotools.worker.steps.VectorizeStep;
import org.geotools.api.data.FileDataStore;
import org.geotools.api.data.FileDataStoreFinder;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.referencing.CRS;

import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageInputStreamSpi;
import javax.imageio.spi.ImageOutputStreamSpi;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ImageWriterSpi;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class GeoToolsWorkerRuntime {

    private static final String READ_SHAPEFILE = "read-shapefile";
    private static final String VECTORIZE = "vectorize";
    private static final String RASTER_RECLASSIFY = "raster-reclassify";

    private GeoToolsWorkerRuntime() {
    }

    public static void execute(String operation, Map<String, String> parameters, List<Double> values) throws Exception {
        refreshImageIoProviders();
        if (READ_SHAPEFILE.equals(operation)) {
            readShapefile(parameters);
            return;
        }
        if (VECTORIZE.equals(operation)) {
            vectorize(parameters, values);
            return;
        }
        if (RASTER_RECLASSIFY.equals(operation)) {
            rasterReclassify(parameters, values);
            return;
        }
        throw new IllegalArgumentException("Unknown GeoTools worker operation: " + operation);
    }

    private static void readShapefile(Map<String, String> parameters) throws Exception {
        GretlLogger log = LogEnvironment.getLogger(GeoToolsWorkerRuntime.class);
        File shapefile = Path.of(require(parameters, "shapefile")).toFile();
        String crsCode = parameters.get("crsCode");
        String taskName = parameters.getOrDefault("taskName", "readShapefile");

        if (!shapefile.exists()) {
            throw new IllegalArgumentException("Shapefile not found: " + shapefile.getAbsolutePath());
        }

        FileDataStore store = FileDataStoreFinder.getDataStore(shapefile);
        if (store == null) {
            throw new IllegalArgumentException("Unable to open shapefile: " + shapefile.getAbsolutePath());
        }
        try {
            SimpleFeatureSource featureSource = store.getFeatureSource();
            CoordinateReferenceSystem crs = null;
            if (crsCode != null && !crsCode.isBlank()) {
                crs = CRS.decode(crsCode, true);
            }

            log.lifecycle(taskName + ":");
            log.lifecycle("  File: " + shapefile.getName());
            log.lifecycle("  Feature count: " + featureSource.getFeatures().size());
            if (crs != null) {
                log.lifecycle("  Target CRS: " + crsCode + " - " + crs.getName());
            }
        } finally {
            store.dispose();
        }
    }

    private static void vectorize(Map<String, String> parameters, List<Double> values) throws Exception {
        Path inputRaster = Path.of(require(parameters, "inputRaster"));
        Path outputGeopackage = Path.of(require(parameters, "outputGeopackage"));
        int band = Integer.parseInt(require(parameters, "band"));
        String taskName = parameters.get("taskName");

        new VectorizeStep(taskName).execute(inputRaster, outputGeopackage, band, values);
    }

    private static void rasterReclassify(Map<String, String> parameters, List<Double> values) throws Exception {
        Path inputRaster = Path.of(require(parameters, "inputRaster"));
        Path outputRaster = Path.of(require(parameters, "outputRaster"));
        double noData = Double.parseDouble(require(parameters, "noData"));
        String taskName = parameters.get("taskName");
        double[] breaks = values.stream().mapToDouble(Double::doubleValue).toArray();

        new RasterReclassifyStep(taskName).execute(inputRaster, outputRaster, breaks, noData);
    }

    private static String require(Map<String, String> parameters, String key) {
        String value = parameters.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing GeoTools worker parameter: " + key);
        }
        return value;
    }

    private static void refreshImageIoProviders() {
        IIORegistry registry = IIORegistry.getDefaultInstance();
        deregisterGeoSolutionsProviders(registry, ImageReaderSpi.class);
        deregisterGeoSolutionsProviders(registry, ImageWriterSpi.class);
        deregisterGeoSolutionsProviders(registry, ImageInputStreamSpi.class);
        deregisterGeoSolutionsProviders(registry, ImageOutputStreamSpi.class);
        ImageIO.scanForPlugins();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void deregisterGeoSolutionsProviders(IIORegistry registry, Class category) {
        List<Object> providersToRemove = new ArrayList<>();
        Iterator<?> providers = registry.getServiceProviders(category, true);
        while (providers.hasNext()) {
            Object provider = providers.next();
            String className = provider.getClass().getName();
            if (className.startsWith("it.geosolutions.") || className.startsWith("org.geotools.")) {
                providersToRemove.add(provider);
            }
        }
        for (Object provider : providersToRemove) {
            registry.deregisterServiceProvider(provider, category);
        }
    }
}
