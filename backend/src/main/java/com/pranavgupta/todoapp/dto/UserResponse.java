package com.pranavgupta.todoapp.dto;

/**
 * Response shape for {@code GET /api/me} — the current authenticated user.
 */
public record UserResponse(
        Long id,
        String email,
        String displayName
) {}
