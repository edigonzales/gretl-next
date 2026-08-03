package ch.so.agi.gretl.job.assertions;

import ch.so.agi.gretl.test.job.TestJobAssertionRegistry;
import java.util.List;

public final class CanonicalTestJobAssertionRegistryFactory {
    public static TestJobAssertionRegistry create() {
        return new TestJobAssertionRegistry(List.of(
                new GzipTestJobAssertions(), new SqliteTestJobAssertions(),
                new CombinedRasterTestJobAssertions(), new ReadShapefileTestJobAssertions(),
                new DuckDbSpatialTestJobAssertions(), new HttpCurlTestJobAssertions(),
                new FtpRoundtripTestJobAssertions(), new S3RoundtripTestJobAssertions(),
                new PostgisSqlTestJobAssertions(), new Ili2duckdbRoundtripTestJobAssertions(),
                new Ili2pgLifecycleTestJobAssertions()));
    }
    private CanonicalTestJobAssertionRegistryFactory() { }
}
