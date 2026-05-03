package com.pranavgupta.todoapp.service;

import com.pranavgupta.todoapp.domain.User;
import com.pranavgupta.todoapp.repository.UserRepository;
import com.pranavgupta.todoapp.security.AppOidcUser;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * On every successful Google OIDC login, upserts the local {@link User} row.
 *
 * <p>Matches by {@code external_id = OIDC sub} (Google's stable subject
 * identifier — never changes for a given Google account). On first login,
 * inserts a fresh {@link User}; on subsequent logins, refreshes
 * {@code email} / {@code displayName} from the latest ID token but never
 * touches {@code external_id}.</p>
 *
 * <p>Returns an {@link AppOidcUser} so downstream controllers can read our
 * app's {@code User.id} from the principal without a per-request DB hit.</p>
 */
@Service
public class CustomOidcUserService extends OidcUserService {

    private final UserRepository userRepository;

    public CustomOidcUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        String externalId = oidcUser.getSubject();   // OIDC "sub" claim
        String email      = oidcUser.getEmail();
        String name       = oidcUser.getFullName();

        User user = userRepository.findByExternalId(externalId)
                .orElseGet(User::new);
        user.setExternalId(externalId);
        user.setEmail(email);
        user.setDisplayName(name);
        user = userRepository.save(user);

        return new AppOidcUser(
                oidcUser.getAuthorities(),
                oidcUser.getIdToken(),
                oidcUser.getUserInfo(),
                user.getId()
        );
    }
}
