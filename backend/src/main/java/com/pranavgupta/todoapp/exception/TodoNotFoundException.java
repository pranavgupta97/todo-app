package com.pranavgupta.todoapp.exception;

/**
 * Thrown when a todo lookup by id fails — either the row doesn't exist, or it
 * exists but is owned by a different user (treated identically from the
 * caller's point of view to avoid leaking the existence of other users' rows).
 *
 * <p>Mapped to a 404 ProblemDetail by {@link GlobalExceptionHandler}.</p>
 */
public class TodoNotFoundException extends RuntimeException {

    private final Long todoId;

    public TodoNotFoundException(Long todoId) {
        super("No todo exists with id " + todoId);
        this.todoId = todoId;
    }

    public Long getTodoId() {
        return todoId;
    }
}
