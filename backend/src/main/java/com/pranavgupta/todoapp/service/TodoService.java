package com.pranavgupta.todoapp.service;

import com.pranavgupta.todoapp.domain.Todo;
import com.pranavgupta.todoapp.dto.TodoStatusFilter;
import com.pranavgupta.todoapp.exception.TodoNotFoundException;
import com.pranavgupta.todoapp.repository.TodoRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic for todos.
 *
 * <p>Every public method is scoped by {@code userId} so that Phase 6 (auth)
 * is a no-change-to-this-class refactor — only the controller's source of
 * {@code userId} changes.</p>
 *
 * <p>Class-level {@code @Transactional(readOnly = true)} sets a sensible
 * default; mutating methods opt back in via {@code @Transactional} (which
 * upgrades to read-write).</p>
 */
@Service
@Transactional(readOnly = true)
public class TodoService {

    private final TodoRepository todoRepository;

    public TodoService(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }

    public List<Todo> findAllForUser(Long userId, TodoStatusFilter filter) {
        return switch (filter) {
            case ALL       -> todoRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
            case ACTIVE    -> todoRepository.findAllByUserIdAndCompletedOrderByCreatedAtDesc(userId, false);
            case COMPLETED -> todoRepository.findAllByUserIdAndCompletedOrderByCreatedAtDesc(userId, true);
        };
    }

    public Todo findByIdForUser(Long userId, Long todoId) {
        return todoRepository.findById(todoId)
                .filter(t -> userId.equals(t.getUserId()))
                .orElseThrow(() -> new TodoNotFoundException(todoId));
    }

    @Transactional
    public Todo createForUser(Long userId, String title) {
        Todo todo = new Todo();
        todo.setTitle(title);
        todo.setUserId(userId);
        // completed defaults to false (primitive boolean field initial value)
        return todoRepository.save(todo);
    }

    @Transactional
    public Todo updateForUser(Long userId, Long todoId, String newTitle, Boolean newCompleted) {
        Todo todo = findByIdForUser(userId, todoId);
        if (newTitle != null) {
            todo.setTitle(newTitle);
        }
        if (newCompleted != null) {
            todo.setCompleted(newCompleted);
        }
        // Hibernate dirty checking flushes on transaction commit — no explicit save needed
        return todo;
    }

    @Transactional
    public void deleteForUser(Long userId, Long todoId) {
        Todo todo = findByIdForUser(userId, todoId);
        todoRepository.delete(todo);
    }
}
