-- ============================================================================
-- V2: Drop the v1 user_id default now that authenticated callers always pass
-- an explicit user_id from the OIDC principal. The DEFAULT 1 was a v1
-- transitional convenience for our auth-unaware code.
--
-- The seeded system user (id=1) remains in place; any v1-era todos that
-- pointed at it stay owned by it. Those rows are harmless orphans (no real
-- person can authenticate as the system user) and can be cleaned up manually
-- if desired. Migrations should not delete user data.
-- ============================================================================

ALTER TABLE todos ALTER COLUMN user_id DROP DEFAULT;
