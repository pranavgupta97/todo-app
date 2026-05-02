package com.pranavgupta.todoapp.dto;

import jakarta.validation.constraints.Size;

/**
 * Request body for {@code PATCH /api/todos/{id}}.
 *
 * <p>Both fields are optional — {@code null} means "do not change". This is
 * standard PATCH semantics: send only what you want to update.</p>
 */
public record UpdateTodoRequest(
        @Size(min = 1, max = 255, message = "title must be 1-255 characters when provided")
        String title,

        Boolean completed
) {}
