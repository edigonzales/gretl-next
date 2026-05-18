package ch.so.agi.gretl.control.server.web;

import ch.so.agi.gretl.control.server.config.GretlControlProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class WorkerAuthentication {
    private final GretlControlProperties properties;

    public WorkerAuthentication(GretlControlProperties properties) {
        this.properties = properties;
    }

    public void requireWorkerToken(HttpServletRequest request) {
        String configured = properties.getSecurity().getWorkerToken();
        String header = request.getHeader("Authorization");
        String token = header != null && header.startsWith("Bearer ") ? header.substring("Bearer ".length()) : null;
        if (configured == null || configured.isBlank() || !configured.equals(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid worker token.");
        }
    }
}
