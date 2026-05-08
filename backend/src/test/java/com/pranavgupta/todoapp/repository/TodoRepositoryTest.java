package com.pranavgupta.todoapp.repository;

import com.pranavgupta.todoapp.TestcontainersConfiguration;
import com.pranavgupta.todoapp.domain.Todo;
import com.pranavgupta.todoapp.domain.User;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

/**
 * Persistence-slice tests for {@link TodoRepository}. Real Postgres via
 * Testcontainers (per {@link TestcontainersConfiguration}); Flyway runs the
 * V1+V2 migrations against the fresh container.
 *
 * <p>{@code @AutoConfigureTestDatabase(replace = NONE)} stops Spring Boot
 * from swapping in an in-memory H2.</p>
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class TodoRepositoryTest {

    @Autowired
    private TodoRepository todoRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void findAllByUserId_returnsOnlyThatUsersTodos() {
        User alice = persistUser("alice-sub", "alice@example.com");
        User bob   = persistUser("bob-sub",   "bob@example.com");

        persistTodo(alice.getId(), "alice's first",  false);
        persistTodo(alice.getId(), "alice's second", false);
        persistTodo(bob.getId(),   "bob's todo",     false);

        List<Todo> aliceTodos = todoRepository.findAllByUserIdOrderByCreatedAtDesc(alice.getId());

        assertThat(aliceTodos)
                .extracting(Todo::getTitle)
                .containsExactlyInAnyOrder("alice's first", "alice's second");
        assertThat(aliceTodos).noneMatch(t -> t.getTitle().equals("bob's todo"));
    }

    @Test
    void findAllByUserIdAndCompleted_filtersOnBothFields() {
        User alice = persistUser("alice-sub", "alice@example.com");

        persistTodo(alice.getId(), "active task",     false);
        persistTodo(alice.getId(), "done task",       true);
        persistTodo(alice.getId(), "another active",  false);

        List<Todo> active = todoRepository
                .findAllByUserIdAndCompletedOrderByCreatedAtDesc(alice.getId(), false);
        List<Todo> done = todoRepository
                .findAllByUserIdAndCompletedOrderByCreatedAtDesc(alice.getId(), true);

        assertThat(active).extracting(Todo::getTitle)
                .containsExactlyInAnyOrder("active task", "another active");
        assertThat(done).extracting(Todo::getTitle).containsExactly("done task");
    }

    @Test
    void findById_returnsTodo_whenExists() {
        User alice = persistUser("alice-sub", "alice@example.com");
        Todo saved = persistTodo(alice.getId(), "find me", false);

        assertThat(todoRepository.findById(saved.getId()))
                .isPresent()
                .get()
                .extracting(Todo::getTitle)
                .isEqualTo("find me");
    }

    @Test
    void findById_returnsEmpty_whenMissing() {
        assertThat(todoRepository.findById(999_999L)).isEmpty();
    }

    // -------------------- helpers --------------------

    private User persistUser(String externalId, String email) {
        User u = new User();
        u.setExternalId(externalId);
        u.setEmail(email);
        u.setDisplayName(email);
        return userRepository.save(u);
    }

    private Todo persistTodo(Long userId, String title, boolean completed) {
        Todo t = new Todo();
        t.setUserId(userId);
        t.setTitle(title);
        t.setCompleted(completed);
        return todoRepository.save(t);
    }
}
