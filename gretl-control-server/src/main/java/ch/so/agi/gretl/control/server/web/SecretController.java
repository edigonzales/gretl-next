package ch.so.agi.gretl.control.server.web;

import ch.so.agi.gretl.control.api.SecretValueRequest;
import ch.so.agi.gretl.control.server.secrets.SecretService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/secrets")
public class SecretController {
    private final SecretService secretService;

    public SecretController(SecretService secretService) {
        this.secretService = secretService;
    }

    @GetMapping
    public List<String> names() {
        return secretService.names();
    }

    @PutMapping("/{name}")
    public void put(@PathVariable String name, @RequestBody SecretValueRequest request) {
        if (request == null || request.value() == null) {
            throw new IllegalArgumentException("Secret value is required.");
        }
        secretService.put(name, request.value());
    }
}
