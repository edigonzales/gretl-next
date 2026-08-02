package ch.so.agi.gretl.test.job;

import java.util.ArrayList;
import java.util.List;

public final class TestJobExecutionSelector {
    public List<TestJobExecutionCase> select(
            TestJobCatalog catalog,
            TestJobExecutionTarget target,
            boolean includeOptional) {
        if (catalog == null || target == null) throw new IllegalArgumentException("catalog and target are required");
        List<TestJobExecutionCase> selected = new ArrayList<>();
        for (TestJobDescriptor descriptor : catalog.all()) {
            TestJobExecutionRequirement jobRequirement = descriptor.requirementFor(target);
            for (TestJobBuildVariant variant : descriptor.builds()) {
                TestJobExecutionDeclaration declaration = variant.declarationFor(target);
                TestJobExecutionRequirement effective = declaration == null
                        ? jobRequirement : declaration.requirement();
                if (effective == TestJobExecutionRequirement.REQUIRED
                        || effective == TestJobExecutionRequirement.OPTIONAL && includeOptional) {
                    selected.add(new TestJobExecutionCase(descriptor, variant, target, effective));
                }
            }
        }
        return List.copyOf(selected);
    }

    public List<TestJobExecutionCase> select(TestJobCatalog catalog, TestJobExecutionTarget target) {
        return select(catalog, target, Boolean.getBoolean("gretl.job.includeOptional"));
    }
}
