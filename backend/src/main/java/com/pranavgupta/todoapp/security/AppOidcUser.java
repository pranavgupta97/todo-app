package com.pranavgupta.todoapp.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import java.io.Serial;
import java.util.Collection;

/**
 * Custom OIDC principal that carries our app's {@code User.id}.
 *
 * <p>Set during {@code CustomOidcUserService.loadUser()} on every login, so
 * controllers can do:</p>
 *
 * <pre>{@code
 *   @GetMapping
 *   List<TodoResponse> list(@AuthenticationPrincipal AppOidcUser principal) {
 *       return todoService.findAllForUser(principal.getAppUserId(), ...);
 *   }
 * }</pre>
 *
 * <p>...without a per-request DB lookup to translate the OIDC {@code sub}
 * back to our internal user id.</p>
 */
public class AppOidcUser extends DefaultOidcUser {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long appUserId;

    public AppOidcUser(
            Collection<? extends GrantedAuthority> authorities,
            OidcIdToken idToken,
            OidcUserInfo userInfo,
            Long appUserId) {
        super(authorities, idToken, userInfo);
        this.appUserId = appUserId;
    }

    /** The {@code users.id} value for this authenticated user. */
    public Long getAppUserId() {
        return appUserId;
    }
}
