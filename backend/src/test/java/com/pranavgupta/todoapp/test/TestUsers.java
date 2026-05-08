package com.pranavgupta.todoapp.test;

import com.pranavgupta.todoapp.domain.User;

import java.time.OffsetDateTime;

/**
 * Test-only factory for {@link User} instances with database-assigned fields
 * populated via reflection. See {@link TestTodos} for the rationale.
 */
public final class TestUsers {

    private TestUsers() {}

    public static User user(Long id, String externalId, String email, String displayName) {
        User u = new User();
        u.setExternalId(externalId);
        u.setEmail(email);
        u.setDisplayName(displayName);
        if (id != null) {
            setField(u, "id", id);
            OffsetDateTime now = OffsetDateTime.now();
            setField(u, "createdAt", now);
            setField(u, "updatedAt", now);
        }
        return u;
    }

    private static void setField(User u, String name, Object value) {
        try {
            var f = User.class.getDeclaredField(name);
            f.setAccessible(true);
            f.set(u, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to set User." + name + " via reflection", e);
        }
    }
}
