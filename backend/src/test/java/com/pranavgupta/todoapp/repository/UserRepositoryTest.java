package com.pranavgupta.todoapp.repository;

import com.pranavgupta.todoapp.TestcontainersConfiguration;
import com.pranavgupta.todoapp.domain.User;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByExternalId_findsExistingUser() {
        User u = new User();
        u.setExternalId("google-sub-123");
        u.setEmail("alice@example.com");
        u.setDisplayName("Alice");
        userRepository.save(u);

        Optional<User> result = userRepository.findByExternalId("google-sub-123");

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("alice@example.com");
        assertThat(result.get().getDisplayName()).isEqualTo("Alice");
    }

    @Test
    void findByExternalId_returnsEmpty_whenMissing() {
        assertThat(userRepository.findByExternalId("nonexistent-sub")).isEmpty();
    }

    @Test
    void seededSystemUser_isPresent() {
        // The V1 migration seeds a system user with id=1, external_id='system'.
        // This belt-check confirms Flyway actually applies V1 to the
        // Testcontainers-backed dev DB during repository tests.
        Optional<User> system = userRepository.findByExternalId("system");
        assertThat(system).isPresent();
        assertThat(system.get().getId()).isEqualTo(1L);
        assertThat(system.get().getEmail()).isEqualTo("system@todo-app.local");
    }
}
