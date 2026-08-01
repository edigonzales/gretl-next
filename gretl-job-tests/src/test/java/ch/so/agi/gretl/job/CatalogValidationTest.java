package ch.so.agi.gretl.job;

import ch.so.agi.gretl.test.job.FileSystemTestJobCatalog;
import ch.so.agi.gretl.test.job.TestJobDescriptor;
import ch.so.agi.gretl.test.job.TestJobExecutionTarget;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CatalogValidationTest {
    @Test
    void discoversTheCompleteCanonicalCatalogWithStableIds() {
        FileSystemTestJobCatalog catalog = FileSystemTestJobCatalog.load(catalogRoot());

        assertEquals(Set.of(
                "combined-core-geotools-pipeline",
                "core-gzip",
                "core-sqlite",
                "geotools-read-shapefile"),
                catalog.all().stream().map(TestJobDescriptor::id).collect(Collectors.toSet()));
        assertEquals(catalog.all().stream().map(TestJobDescriptor::id).sorted().toList(),
                catalog.all().stream().map(TestJobDescriptor::id).toList());
        assertFalse(catalog.all().isEmpty());
        for (TestJobDescriptor job : catalog.all()) {
            for (TestJobExecutionTarget target : TestJobExecutionTarget.values()) {
                job.requirementFor(target);
            }
        }
    }

    private static Path catalogRoot() {
        String value = System.getProperty("gretl.test.jobsRoot");
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing gretl.test.jobsRoot");
        }
        return Path.of(value);
    }
}
