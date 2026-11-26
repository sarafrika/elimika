-- A guardian with a Sarafrika account can add a child as a learner without giving the child a login.
-- The child's users row has no Keycloak account and so no email; every account that can sign in keeps one.
ALTER TABLE users
    ALTER COLUMN email DROP NOT NULL;

ALTER TABLE users
    DROP CONSTRAINT IF EXISTS chk_users_email_required_for_login;

ALTER TABLE users
    ADD CONSTRAINT chk_users_email_required_for_login
        CHECK (email IS NOT NULL OR keycloak_id IS NULL);
