package ch.so.agi.gretl.control.server.web;

import ch.so.agi.gretl.control.server.config.GretlControlProperties;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UiAccessService {
    private final GretlControlProperties properties;

    public UiAccessService(GretlControlProperties properties) {
        this.properties = properties;
    }

    public boolean canOperate(Authentication authentication) {
        return hasAnyRole(authentication, "ROLE_OPERATOR", "ROLE_ADMIN");
    }

    public boolean canAdmin(Authentication authentication) {
        return hasAnyRole(authentication, "ROLE_ADMIN");
    }

    private boolean hasAnyRole(Authentication authentication, String... roles) {
        if (!properties.getSecurity().isOidcEnabled()) {
            return true;
        }
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toUnmodifiableSet());
        for (String role : roles) {
            if (authorities.contains(role)) {
                return true;
            }
        }
        return false;
    }
}
