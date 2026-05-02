package com.pranavgupta.todoapp.dto;

import com.pranavgupta.todoapp.domain.Todo;

import java.time.OffsetDateTime;

/**
 * Response shape for a single {@link Todo} returned by the API.
 *
 * <p>{@code userId} is intentionally not exposed — the API treats the current
 * user's data as the only data the caller can ever see, so leaking the id
 * would just be noise.</p>
 */
public record TodoResponse(
        Long id,
        String title,
        boolean completed,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static TodoResponse from(Todo todo) {
        return new TodoResponse(
                todo.getId(),
                todo.getTitle(),
                todo.isCompleted(),
                todo.getCreatedAt(),
                todo.getUpdatedAt()
        );
    }
}
