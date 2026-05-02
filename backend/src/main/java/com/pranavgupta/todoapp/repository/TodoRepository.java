package com.pranavgupta.todoapp.repository;

import com.pranavgupta.todoapp.domain.Todo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository for {@link Todo}.
 *
 * <p>All read methods are scoped by {@code userId} so that Phase 6 (auth)
 * naturally enforces per-user authorization without changing the repository
 * surface.</p>
 */
public interface TodoRepository extends JpaRepository<Todo, Long> {

    List<Todo> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    List<Todo> findAllByUserIdAndCompletedOrderByCreatedAtDesc(Long userId, boolean completed);
}
