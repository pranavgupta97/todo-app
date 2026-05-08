package com.pranavgupta.todoapp.service;

import com.pranavgupta.todoapp.domain.User;
import com.pranavgupta.todoapp.repository.UserRepository;
import com.pranavgupta.todoapp.security.AppOidcUser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.pranavgupta.todoapp.test.TestUsers.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CustomOidcUserService}'s upsert behaviour.
 *
 * <p>The HTTP-bound {@code super.loadUser} call (which contacts the IdP) is
 * sidestepped by testing the package-private {@code upsertAndWrap} seam
 * directly. We hand it a pre-built {@link OidcUser} that already has the
 * claims our service reads.</p>
 */
@ExtendWith(MockitoExtension.class)
class CustomOidcUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomOidcUserService service;

    @Test
    void upsertAndWrap_firstLogin_insertsNewUserAndReturnsAppOidcUser() {
        when(userRepository.findByExternalId("google-sub-123")).thenReturn(Optional.empty());
        // Simulate Hibernate assigning the id on save:
        when(userRepository.save(any(User.class))).thenAnswer(inv ->
                user(42L, ((User) inv.getArgument(0)).getExternalId(),
                          ((User) inv.getArgument(0)).getEmail(),
                          ((User) inv.getArgument(0)).getDisplayName()));

        OidcUser fromGoogle = oidcUser("google-sub-123", "alice@example.com", "Alice");

        AppOidcUser result = service.upsertAndWrap(fromGoogle);

        // The new User row was saved with the right fields...
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        org.mockito.Mockito.verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getExternalId()).isEqualTo("google-sub-123");
        assertThat(saved.getEmail()).isEqualTo("alice@example.com");
        assertThat(saved.getDisplayName()).isEqualTo("Alice");

        // ...and the returned principal carries the right appUserId + claims.
        assertThat(result.getAppUserId()).isEqualTo(42L);
        assertThat(result.getEmail()).isEqualTo("alice@example.com");
        assertThat(result.getFullName()).isEqualTo("Alice");
        assertThat(result.getSubject()).isEqualTo("google-sub-123");
    }

    @Test
    void upsertAndWrap_subsequentLogin_refreshesEmailAndDisplayName_butNotExternalId() {
        // Existing user has stale email + name from a previous Google profile snapshot.
        User existing = user(99L, "google-sub-123", "old@example.com", "Old Name");
        when(userRepository.findByExternalId("google-sub-123")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        OidcUser refreshedFromGoogle = oidcUser("google-sub-123", "new@example.com", "New Name");

        AppOidcUser result = service.upsertAndWrap(refreshedFromGoogle);

        // The existing User row was mutated in place:
        assertThat(existing.getEmail()).isEqualTo("new@example.com");
        assertThat(existing.getDisplayName()).isEqualTo("New Name");
        assertThat(existing.getExternalId()).isEqualTo("google-sub-123"); // unchanged
        // ...and the returned principal points at the same user.id (99L) — not a new row.
        assertThat(result.getAppUserId()).isEqualTo(99L);
    }

    /** Build a minimal {@link OidcUser} with the claims our service reads. */
    private static OidcUser oidcUser(String sub, String email, String fullName) {
        Map<String, Object> claims = Map.of(
                "sub",   sub,
                "email", email,
                "name",  fullName
        );
        OidcIdToken idToken = new OidcIdToken(
                "fake-token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                claims
        );
        OidcUserInfo userInfo = new OidcUserInfo(claims);
        return new DefaultOidcUser(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                idToken,
                userInfo
        );
    }
}
