package ch.so.agi.gretl.test.job;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface TestJobCatalog {
    List<TestJobDescriptor> all();
    Optional<TestJobDescriptor> find(String id);
    TestJobDescriptor require(String id);
    Stream<TestJobDescriptor> supporting(TestJobExecutionTarget target);
    Path rootDirectory();
}
