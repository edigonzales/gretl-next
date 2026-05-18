package ch.so.agi.gretl.control.server.schedule;

import ch.so.agi.gretl.control.server.run.RunService;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

public class GretlQuartzJob extends QuartzJobBean {
    static final String JOB_ID = "jobId";

    @Autowired
    private RunService runService;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        String jobId = context.getMergedJobDataMap().getString(JOB_ID);
        if (jobId == null || jobId.isBlank()) {
            throw new JobExecutionException("Scheduled GRETL job is missing jobId.");
        }
        runService.enqueueScheduled(jobId);
    }
}
