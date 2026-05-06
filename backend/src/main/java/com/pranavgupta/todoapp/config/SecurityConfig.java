package com.pranavgupta.todoapp.config;

import com.pranavgupta.todoapp.service.CustomOidcUserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/**
 * Spring Security configuration.
 *
 * <ul>
 *   <li><b>Public endpoints:</b> {@code /actuator/health/**}, {@code /error},
 *       {@code /login/**}, {@code /oauth2/**} — needed for k8s/Fly probes
 *       and Spring Security's own OAuth2 dance.</li>
 *   <li><b>Everything else (incl. {@code /api/**})</b>: requires an
 *       authenticated session.</li>
 *   <li><b>OIDC login</b> via Google. {@link CustomOidcUserService} upserts
 *       the local {@code User} row on every login.</li>
 *   <li><b>Logout:</b> {@code POST /logout} (Spring's default), redirects
 *       to {@code /} after.</li>
 *   <li><b>CSRF:</b> profile-aware via {@code app.security.csrf.enabled}.
 *       <ul>
 *         <li><b>Default {@code true}</b> (base config, test profile, prod):
 *             enabled with a {@code CookieCsrfTokenRepository} configured
 *             to be readable by JavaScript so the Phase 11 frontend can
 *             read the {@code XSRF-TOKEN} cookie and echo it as
 *             {@code X-XSRF-TOKEN} on mutating requests.</li>
 *         <li><b>Override {@code false} in dev profile</b>: removes the
 *             token requirement so manual API testing via {@code .http}
 *             files only needs a session cookie. CSRF on {@code localhost}
 *             defends against attackers that don't exist in dev; tests +
 *             prod still exercise the protection.</li>
 *       </ul>
 *   </li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final CustomOidcUserService customOidcUserService;
    private final boolean csrfEnabled;

    public SecurityConfig(
            CustomOidcUserService customOidcUserService,
            @Value("${app.security.csrf.enabled:true}") boolean csrfEnabled) {
        this.customOidcUserService = customOidcUserService;
        this.csrfEnabled = csrfEnabled;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/health/**",
                                "/error",
                                "/login/**",
                                "/oauth2/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(login -> login
                        .userInfoEndpoint(ui -> ui.oidcUserService(customOidcUserService))
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                        .permitAll()
                );

        if (csrfEnabled) {
            http.csrf(csrf -> csrf
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()));
        } else {
            // Loud warning so this can't be missed if someone accidentally enables
            // the dev profile in a non-local environment.
            log.warn("CSRF protection is DISABLED (app.security.csrf.enabled=false). "
                    + "This should ONLY be active in the local dev profile.");
            http.csrf(csrf -> csrf.disable());
        }

        return http.build();
    }
}
