package ch.so.agi.gretl.control.server.web;

import ch.so.agi.gretl.control.manifest.JobDefinition;
import ch.so.agi.gretl.control.manifest.ManifestException;
import ch.so.agi.gretl.control.manifest.ParameterDefinition;
import ch.so.agi.gretl.control.server.manifest.ManifestAdministrationService;
import ch.so.agi.gretl.control.server.persistence.RunRecord;
import ch.so.agi.gretl.control.server.run.RunService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
public class UiController {
    private final ControlUiService uiService;
    private final UiAccessService accessService;
    private final RunService runService;
    private final ManifestAdministrationService manifestAdministrationService;

    public UiController(
            ControlUiService uiService,
            UiAccessService accessService,
            RunService runService,
            ManifestAdministrationService manifestAdministrationService) {
        this.uiService = uiService;
        this.accessService = accessService;
        this.runService = runService;
        this.manifestAdministrationService = manifestAdministrationService;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/jobs";
    }

    @GetMapping("/jobs")
    public String jobs(Model model, Authentication authentication) {
        addAccess(model, authentication, "jobs");
        model.addAttribute("jobSummaries", uiService.jobSummaries());
        return "ui/jobs";
    }

    @GetMapping("/jobs/{jobId}")
    public String jobDetail(@PathVariable String jobId, Model model, Authentication authentication) {
        addJobDetail(model, authentication, jobId);
        return "ui/job-detail";
    }

    @PostMapping("/jobs/{jobId}/runs")
    public String startJob(
            @PathVariable String jobId,
            @RequestParam Map<String, String> requestParameters,
            Model model,
            Authentication authentication,
            HttpServletResponse response) {
        if (!accessService.canOperate(authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Starting jobs requires operator permissions.");
        }
        JobDefinition job = uiService.requireJob(jobId);
        try {
            RunRecord run = runService.enqueueManual(jobId, startParameters(job, requestParameters), actor(authentication));
            model.addAttribute("startRun", uiService.runSummary(run));
            model.addAttribute("startMessage", "Queued run " + run.id() + ".");
            response.addHeader("HX-Trigger", "runStarted");
        } catch (ManifestException | ResponseStatusException | IllegalArgumentException exception) {
            model.addAttribute("startError", userMessage(exception));
        }
        model.addAttribute("job", job);
        return "ui/job-fragments :: startResult";
    }

    @GetMapping("/runs/{runId}/logs")
    public String runLogs(@PathVariable String runId, Model model) {
        RunRecord run = runService.requireRun(runId);
        model.addAttribute("runSummary", uiService.runSummary(run));
        model.addAttribute("log", runService.readLog(runId));
        model.addAttribute("streamLogs", uiService.isActive(run));
        return "ui/job-fragments :: runLog";
    }

    @GetMapping("/admin")
    public String admin(Model model, Authentication authentication) {
        addAccess(model, authentication, "admin");
        addAdminModel(model);
        return "ui/admin";
    }

    @PostMapping("/admin/manifest/reload")
    public String reloadManifest(Model model, Authentication authentication, HttpServletResponse response) {
        if (!accessService.canAdmin(authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Reloading the catalog requires admin permissions.");
        }
        addAccess(model, authentication, "admin");
        model.addAttribute("manifestReload", manifestAdministrationService.reload());
        model.addAttribute("manifestStatus", manifestAdministrationService.status());
        response.addHeader("HX-Trigger", "manifestReloaded");
        return "ui/admin-fragments :: manifestPanel";
    }

    @GetMapping("/ui/fragments/jobs")
    public String jobSummaries(Model model) {
        model.addAttribute("jobSummaries", uiService.jobSummaries());
        return "ui/fragments :: jobSummaryTable";
    }

    @GetMapping("/ui/fragments/jobs/{jobId}/runs")
    public String runHistory(@PathVariable String jobId, Model model, Authentication authentication) {
        model.addAttribute("job", uiService.requireJob(jobId));
        model.addAttribute("runs", uiService.runSummariesForJob(jobId));
        model.addAttribute("canOperate", accessService.canOperate(authentication));
        return "ui/job-fragments :: runHistory";
    }

    @GetMapping("/ui/fragments/admin/manifest")
    public String manifestStatus(Model model, Authentication authentication) {
        addAccess(model, authentication, "admin");
        model.addAttribute("manifestStatus", manifestAdministrationService.status());
        return "ui/admin-fragments :: manifestPanel";
    }

    @GetMapping("/ui/fragments/admin/workers")
    public String workerStatus(Model model) {
        model.addAttribute("workers", uiService.workerSummaries());
        return "ui/admin-fragments :: workerPanel";
    }

    private void addJobDetail(Model model, Authentication authentication, String jobId) {
        addAccess(model, authentication, "jobs");
        model.addAttribute("job", uiService.requireJob(jobId));
        model.addAttribute("runs", uiService.runSummariesForJob(jobId));
    }

    private void addAdminModel(Model model) {
        model.addAttribute("manifestStatus", manifestAdministrationService.status());
        model.addAttribute("workers", uiService.workerSummaries());
    }

    private void addAccess(Model model, Authentication authentication, String activePage) {
        model.addAttribute("activePage", activePage);
        model.addAttribute("canOperate", accessService.canOperate(authentication));
        model.addAttribute("canAdmin", accessService.canAdmin(authentication));
    }

    private Map<String, Object> startParameters(JobDefinition job, Map<String, String> requestParameters) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        for (ParameterDefinition parameter : job.parameters()) {
            String value = requestParameters.get(parameter.name());
            if (value != null && !value.isBlank()) {
                parameters.put(parameter.name(), value);
            }
        }
        return parameters;
    }

    private String actor(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
            return "anonymous";
        }
        return authentication.getName();
    }

    private String userMessage(RuntimeException exception) {
        if (exception instanceof ResponseStatusException responseStatusException && responseStatusException.getReason() != null) {
            return responseStatusException.getReason();
        }
        return exception.getMessage();
    }
}
