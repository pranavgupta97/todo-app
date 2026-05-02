package com.pranavgupta.todoapp.repository;

import com.pranavgupta.todoapp.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for {@link User}.
 *
 * <p>v1 only reads the seeded system user; Phase 6 (auth) will use
 * {@link #findByExternalId(String)} to upsert OIDC users on first sign-in.</p>
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByExternalId(String externalId);
}
