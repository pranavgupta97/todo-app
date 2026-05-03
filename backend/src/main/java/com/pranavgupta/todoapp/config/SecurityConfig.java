package com.pranavgupta.todoapp.config;

import com.pranavgupta.todoapp.service.CustomOidcUserService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/**
 * Spring Security configuration.
 *
 * <ul>
 *   <li><b>Public endpoints:</b> {@code /actuator/health/**}, {@code /error},
 *       {@code /login/**}, {@code /oauth2/**} — these are needed for k8s/Fly
 *       probes and Spring Security's own OAuth2 dance.</li>
 *   <li><b>Everything else (incl. {@code /api/**})</b>: requires an
 *       authenticated session.</li>
 *   <li><b>OIDC login</b> via Google. {@link CustomOidcUserService} upserts
 *       the local {@code User} row on every login.</li>
 *   <li><b>Logout:</b> {@code POST /logout} (Spring's default, CSRF-protected),
 *       redirects to {@code /} after.</li>
 *   <li><b>CSRF:</b> enabled with a {@code CookieCsrfTokenRepository}
 *       configured to be readable by JavaScript (the Phase 11 frontend will
 *       read the {@code XSRF-TOKEN} cookie and echo it as
 *       {@code X-XSRF-TOKEN} on mutating requests).</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomOidcUserService customOidcUserService;

    public SecurityConfig(CustomOidcUserService customOidcUserService) {
        this.customOidcUserService = customOidcUserService;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
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
                )
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                )
                .build();
    }
}
