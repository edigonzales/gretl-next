package ch.so.agi.gretl.control.server.schedule;

import ch.so.agi.gretl.control.manifest.JobDefinition;
import ch.so.agi.gretl.control.server.config.GretlControlProperties;
import ch.so.agi.gretl.control.server.manifest.ManifestCatalog;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

@Component
public class QuartzScheduleSynchronizer {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuartzScheduleSynchronizer.class);
    public static final String GROUP = "gretl-control-jobs";

    private final Scheduler scheduler;
    private final ManifestCatalog catalog;
    private final GretlControlProperties properties;

    public QuartzScheduleSynchronizer(Scheduler scheduler, ManifestCatalog catalog, GretlControlProperties properties) {
        this.scheduler = scheduler;
        this.catalog = catalog;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void synchronize() throws SchedulerException {
        synchronize(catalog.jobs());
    }

    public synchronized void synchronize(List<JobDefinition> jobs) throws SchedulerException {
        Set<JobKey> manifestJobKeys = new HashSet<>();
        for (JobDefinition job : jobs) {
            manifestJobKeys.add(JobKey.jobKey(job.id(), GROUP));
        }
        for (JobKey existingJobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(GROUP))) {
            if (!manifestJobKeys.contains(existingJobKey)) {
                scheduler.deleteJob(existingJobKey);
                LOGGER.info("Removed Quartz schedule for GRETL job {} because it is no longer in the manifest.",
                        existingJobKey.getName());
            }
        }

        for (JobDefinition job : jobs) {
            JobKey jobKey = JobKey.jobKey(job.id(), GROUP);
            TriggerKey triggerKey = TriggerKey.triggerKey(job.id(), GROUP);
            scheduler.deleteJob(jobKey);
            if (!job.isEnabled() || job.cron() == null || job.cron().isBlank()) {
                LOGGER.info("GRETL job {} has no active Quartz schedule.", job.id());
                continue;
            }
            JobDataMap data = new JobDataMap();
            data.put(GretlQuartzJob.JOB_ID, job.id());
            scheduler.scheduleJob(
                    JobBuilder.newJob(GretlQuartzJob.class)
                            .withIdentity(jobKey)
                            .usingJobData(data)
                            .build(),
                    TriggerBuilder.newTrigger()
                            .withIdentity(triggerKey)
                            .withSchedule(CronScheduleBuilder.cronSchedule(job.cron())
                                    .inTimeZone(TimeZone.getTimeZone(job.timezone() == null || job.timezone().isBlank()
                                            ? properties.getDefaultTimezone()
                                            : job.timezone()))
                                    .withMisfireHandlingInstructionDoNothing())
                            .build());
            LOGGER.info("Scheduled GRETL job {} with cron '{}'.", job.id(), job.cron());
        }
    }
}
