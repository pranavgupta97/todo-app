package com.pranavgupta.todoapp.test;

import com.pranavgupta.todoapp.security.AppOidcUser;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Test-only factory for {@link AppOidcUser} principals.
 *
 * <p>Use with {@code SecurityMockMvcRequestPostProcessors.oidcLogin().oidcUser(...)}
 * in {@code @WebMvcTest} and {@code @SpringBootTest} controller tests so the
 * controllers receive a real {@link AppOidcUser} (and can read
 * {@code principal.getAppUserId()}) — not the generic {@code DefaultOidcUser}
 * you'd get from {@code @WithMockUser}.</p>
 */
public final class TestPrincipals {

    private TestPrincipals() {}

    /** Convenience: synthesises email/name from the user id. */
    public static AppOidcUser appOidcUser(Long appUserId) {
        return appOidcUser(appUserId, "user-" + appUserId + "@example.com", "User " + appUserId);
    }

    public static AppOidcUser appOidcUser(Long appUserId, String email, String fullName) {
        Map<String, Object> claims = Map.of(
                "sub",   "google-sub-" + appUserId,
                "email", email,
                "name",  fullName
        );
        OidcIdToken idToken = new OidcIdToken(
                "fake-id-token-" + appUserId,
                Instant.now(),
                Instant.now().plusSeconds(3600),
                claims
        );
        OidcUserInfo userInfo = new OidcUserInfo(claims);

        return new AppOidcUser(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                idToken,
                userInfo,
                appUserId
        );
    }
}
