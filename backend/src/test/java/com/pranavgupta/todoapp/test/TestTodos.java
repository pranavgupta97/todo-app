package com.pranavgupta.todoapp.test;

import com.pranavgupta.todoapp.domain.Todo;

import java.time.OffsetDateTime;

/**
 * Test-only factory for {@link Todo} instances with database-assigned fields
 * (id, createdAt, updatedAt) populated via reflection.
 *
 * <p>The production {@code Todo} entity exposes only getters for {@code id} /
 * timestamps because they are managed by Hibernate / {@code @PrePersist}; in
 * unit tests where there's no real persistence layer, we still want to
 * simulate "this row was already saved" for assertions about JSON output and
 * {@code Location} headers.</p>
 */
public final class TestTodos {

    private TestTodos() {}

    /** Builds a Todo with id, userId, title, completed=false, and timestamps. */
    public static Todo todo(Long id, Long userId, String title) {
        return todo(id, userId, title, false);
    }

    public static Todo todo(Long id, Long userId, String title, boolean completed) {
        Todo t = new Todo();
        t.setUserId(userId);
        t.setTitle(title);
        t.setCompleted(completed);
        if (id != null) {
            setField(t, "id", id);
            OffsetDateTime now = OffsetDateTime.now();
            setField(t, "createdAt", now);
            setField(t, "updatedAt", now);
        }
        return t;
    }

    /** Build a transient (unsaved) Todo — no id, no timestamps. */
    public static Todo transientTodo(Long userId, String title) {
        return todo(null, userId, title);
    }

    private static void setField(Todo t, String name, Object value) {
        try {
            var f = Todo.class.getDeclaredField(name);
            f.setAccessible(true);
            f.set(t, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to set Todo." + name + " via reflection", e);
        }
    }
}
