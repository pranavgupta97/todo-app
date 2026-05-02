package com.pranavgupta.todoapp.dto;

/**
 * Filter for {@code GET /api/todos?status=...}.
 *
 * <p>Bound from the query string by {@code TodoStatusFilterConverter}
 * (in the {@code config} package) so callers can use {@code ?status=all},
 * {@code ?status=Active}, etc. — case-insensitive.</p>
 */
public enum TodoStatusFilter {
    ALL, ACTIVE, COMPLETED;

    /**
     * Case-insensitive parser. Returns {@link #ALL} for null/blank input.
     *
     * @throws IllegalArgumentException if {@code raw} doesn't match any enum
     *         constant; the {@code GlobalExceptionHandler} maps this to a
     *         400 ProblemDetail with a helpful message.
     */
    public static TodoStatusFilter from(String raw) {
        if (raw == null || raw.isBlank()) {
            return ALL;
        }
        try {
            return TodoStatusFilter.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Unknown status filter: '" + raw + "'. Valid values: all, active, completed.");
        }
    }
}
