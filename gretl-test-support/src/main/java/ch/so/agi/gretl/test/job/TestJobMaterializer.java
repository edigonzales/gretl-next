package ch.so.agi.gretl.test.job;

import java.nio.file.Path;

public interface TestJobMaterializer {
    MaterializedTestJob materialize(TestJobDescriptor descriptor, TestJobBuildVariant build,
                                    TestJobExecutionTarget target, Path destinationRoot);
}
