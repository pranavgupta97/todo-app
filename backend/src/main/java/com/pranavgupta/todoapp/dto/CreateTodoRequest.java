package com.pranavgupta.todoapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /api/todos}.
 */
public record CreateTodoRequest(
        @NotBlank(message = "title must not be blank")
        @Size(max = 255, message = "title must not exceed 255 characters")
        String title
) {}
