-- ============================================================================
-- V1: Initial schema for the Todo App.
--
-- Designed forward to support OIDC authentication (Phase 6) without ever
-- back-adding a NOT NULL FK column to a populated table. v1 inserts a single
-- system user (id=1) so todos can default to it; the auth phase will issue a
-- V2 migration that drops the user_id default once code starts passing the
-- authenticated user_id explicitly.
-- ============================================================================

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    -- OIDC subject identifier ("sub" claim from the ID token). Nullable for
    -- the system user; populated for real users once Phase 6 (auth) lands.
    external_id   VARCHAR(255) UNIQUE,
    email         VARCHAR(255) UNIQUE,
    display_name  VARCHAR(255),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Seed the system user so v1 todos (which don't yet know about auth) have a
-- valid FK target. The auth phase keeps this row in place; real users are
-- inserted via the OIDC sign-in flow.
INSERT INTO users (id, external_id, email, display_name)
VALUES (1, 'system', 'system@todo-app.local', 'System User');

-- Reset the sequence so the next inserted user (a real OIDC user) gets id=2,
-- not id=1. Without this, the next BIGSERIAL value would still be 1 and the
-- INSERT would collide on the primary key.
SELECT setval('users_id_seq', 1, true);

CREATE TABLE todos (
    id          BIGSERIAL PRIMARY KEY,
    -- DEFAULT 1 lets v1 code insert without specifying user_id; the auth
    -- phase will issue V2 to drop this default.
    user_id     BIGINT NOT NULL DEFAULT 1
                  REFERENCES users(id) ON DELETE CASCADE,
    title       VARCHAR(255) NOT NULL,
    completed   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Composite index supports both per-user listing and per-user filtering by
-- completion status — the two main read patterns from the API.
CREATE INDEX idx_todos_user_completed ON todos(user_id, completed);
