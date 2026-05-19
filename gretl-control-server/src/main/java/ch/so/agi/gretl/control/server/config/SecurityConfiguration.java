package ch.so.agi.gretl.control.server.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.web.SecurityFilterChain;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Configuration
public class SecurityConfiguration {
    @Bean
    @ConditionalOnProperty(name = "gretl.control.security.oidc-enabled", havingValue = "true")
    SecurityFilterChain oidcSecurityFilterChain(HttpSecurity http, GrantedAuthoritiesMapper authoritiesMapper) throws Exception {
        return http
                .authorizeHttpRequests(registry -> registry
                        .requestMatchers("/", "/index.html", "/app.js", "/styles.css").permitAll()
                        .requestMatchers("/api/worker/**").permitAll()
                        .requestMatchers("/api/secrets/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/admin/manifest").hasAnyRole("VIEWER", "OPERATOR", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/admin/manifest/reload").hasRole("ADMIN")
                        .requestMatchers("/api/jobs/*/runs", "/api/runs/*/cancel", "/api/runs/*/retry").hasAnyRole("OPERATOR", "ADMIN")
                        .requestMatchers("/api/**").hasAnyRole("VIEWER", "OPERATOR", "ADMIN")
                        .anyRequest().authenticated())
                .oauth2Login(oauth -> oauth.userInfoEndpoint(userInfo -> userInfo.userAuthoritiesMapper(authoritiesMapper)))
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                .build();
    }

    @Bean
    GrantedAuthoritiesMapper oidcAuthoritiesMapper() {
        return authorities -> {
            Set<GrantedAuthority> mapped = new HashSet<>(authorities);
            for (GrantedAuthority authority : authorities) {
                if (authority instanceof OAuth2UserAuthority oauth2Authority) {
                    mapClaimValues(mapped, oauth2Authority, "roles");
                    mapClaimValues(mapped, oauth2Authority, "groups");
                }
            }
            return mapped;
        };
    }

    @Bean
    @ConditionalOnProperty(name = "gretl.control.security.oidc-enabled", havingValue = "false", matchIfMissing = true)
    SecurityFilterChain developmentSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(registry -> registry.anyRequest().permitAll())
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                .build();
    }

    private void mapClaimValues(Set<GrantedAuthority> mapped, OAuth2UserAuthority authority, String claimName) {
        Object claim = authority.getAttributes().get(claimName);
        if (claim instanceof Iterable<?> values) {
            for (Object value : values) {
                mapRole(mapped, value);
            }
        } else {
            mapRole(mapped, claim);
        }
    }

    private void mapRole(Set<GrantedAuthority> mapped, Object value) {
        if (value == null) {
            return;
        }
        String role = value.toString().trim();
        if (role.isEmpty()) {
            return;
        }
        mapped.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase(Locale.ROOT)));
    }
}
