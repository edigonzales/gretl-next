package ch.so.agi.gretl.job.assertions;

import ch.so.agi.gretl.test.job.CommonTestJobAssertions;
import ch.so.agi.gretl.test.job.TestJobAssertions;
import ch.so.agi.gretl.test.job.TestJobVerificationContext;

public abstract class AbstractCanonicalTestJobAssertions implements TestJobAssertions {
    @Override
    public final void verify(TestJobVerificationContext context) throws Exception {
        CommonTestJobAssertions.assertSuccessful(context.result());
        CommonTestJobAssertions.assertNoClassloaderFailure(context.result());
        CommonTestJobAssertions.assertNoWorkerProtocolLeak(context.result());
        CommonTestJobAssertions.assertSecretsAbsent(context.result(),
                context.environment().environment().secretValues());
        if (context.job().target().name().startsWith("RUNTIME_IMAGE_")) {
            CommonTestJobAssertions.assertNoRemoteDownloadLog(context.result());
        }
        verifyJob(context);
    }

    protected abstract void verifyJob(TestJobVerificationContext context) throws Exception;
}
