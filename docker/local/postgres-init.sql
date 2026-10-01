-- Runs once, when the elimika_local_pg volume is first initialised (docker/compose.local.yaml).
-- Keycloak keeps its realm in its own database on the same local server. Local-only credentials.
CREATE ROLE keycloak LOGIN PASSWORD 'keycloak-local';
CREATE DATABASE keycloak OWNER keycloak;
