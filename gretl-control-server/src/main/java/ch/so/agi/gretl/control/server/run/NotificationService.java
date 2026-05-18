package ch.so.agi.gretl.control.server.run;

import ch.so.agi.gretl.control.api.RunStatus;
import ch.so.agi.gretl.control.manifest.JobDefinition;
import ch.so.agi.gretl.control.manifest.NotificationDefinition;
import ch.so.agi.gretl.control.server.persistence.RunRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class NotificationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationService.class);

    private final Optional<JavaMailSender> mailSender;
    private final RestClient restClient;

    public NotificationService(Optional<JavaMailSender> mailSender, RestClient.Builder restClientBuilder) {
        this.mailSender = mailSender;
        this.restClient = restClientBuilder.build();
    }

    public void notify(JobDefinition job, RunRecord run) {
        for (NotificationDefinition notification : job.notifications()) {
            if (!matches(notification, run.status())) {
                continue;
            }
            sendMail(job, run, notification);
            sendWebhook(job, run, notification);
        }
    }

    private boolean matches(NotificationDefinition notification, RunStatus status) {
        if (notification.on().isEmpty()) {
            return status == RunStatus.FAILED || status == RunStatus.TIMED_OUT || status == RunStatus.CANCELLED;
        }
        String token = status.name().toLowerCase(Locale.ROOT);
        return notification.on().stream().map(value -> value.toLowerCase(Locale.ROOT)).anyMatch(token::equals);
    }

    private void sendMail(JobDefinition job, RunRecord run, NotificationDefinition notification) {
        if (notification.email() == null || notification.email().isBlank()) {
            return;
        }
        if (mailSender.isEmpty()) {
            LOGGER.info("Mail notification for run {} suppressed because no JavaMailSender is configured.", run.id());
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(notification.email());
        message.setSubject("GRETL job " + job.id() + " " + run.status());
        message.setText("Job: %s%nRun: %s%nStatus: %s%nMessage: %s%n".formatted(
                job.id(), run.id(), run.status(), run.message() == null ? "" : run.message()));
        mailSender.get().send(message);
    }

    private void sendWebhook(JobDefinition job, RunRecord run, NotificationDefinition notification) {
        if (notification.webhook() == null || notification.webhook().isBlank()) {
            return;
        }
        try {
            restClient.post()
                    .uri(notification.webhook())
                    .body(Map.of(
                            "jobId", job.id(),
                            "runId", run.id(),
                            "status", run.status().name(),
                            "message", run.message() == null ? "" : run.message()))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException e) {
            LOGGER.warn("Webhook notification for run {} failed: {}", run.id(), e.getMessage());
        }
    }
}
