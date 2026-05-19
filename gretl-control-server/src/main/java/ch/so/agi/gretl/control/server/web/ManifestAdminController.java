package ch.so.agi.gretl.control.server.web;

import ch.so.agi.gretl.control.server.manifest.ManifestAdministrationService;
import ch.so.agi.gretl.control.server.manifest.ManifestReloadResponse;
import ch.so.agi.gretl.control.server.manifest.ManifestStatusResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/manifest")
public class ManifestAdminController {
    private final ManifestAdministrationService manifestAdministrationService;

    public ManifestAdminController(ManifestAdministrationService manifestAdministrationService) {
        this.manifestAdministrationService = manifestAdministrationService;
    }

    @GetMapping
    public ManifestStatusResponse status() {
        return manifestAdministrationService.status();
    }

    @PostMapping("/reload")
    public ManifestReloadResponse reload() {
        return manifestAdministrationService.reload();
    }
}
