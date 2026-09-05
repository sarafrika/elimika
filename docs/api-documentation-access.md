# API Documentation Access — Configuration Runbook

## What changed

`/v3/api-docs` and the Swagger UI used to be `permitAll`, so the complete specification —
every route, parameter, payload and enum, including the admin and moderation surface — was
readable by anyone who could reach the host. It is now treated as internal documentation.

| Path group | Before | After |
|---|---|---|
| `/v3/api-docs`, `/v3/api-docs/**`, `/v3/api-docs.yaml`, `/v3/api-docs.yaml/**`, `/v2/api-docs`, `/swagger-ui/**`, `/swagger-ui.html`, `/swagger-resources/**`, `/configuration/**`, `/webjars/**` | anonymous | anonymous **only** when `APP_API_DOCS_PUBLIC=true`; otherwise platform administrators only |
| `/actuator/health`, `/actuator/health/**`, `/health/**` | anonymous | anonymous (unchanged — the container healthcheck in `docker/compose.yaml` curls it without credentials) |
| `/actuator/**` — i.e. `/actuator/info`, `/actuator/metrics` | anonymous | authenticated |

### Why the specification patterns are derived, not listed

springdoc registers **four** handler mappings off the single `springdoc.api-docs.path` property
(verified against springdoc-openapi 2.8.17):

| Mapping | Handler | Resolves to |
|---|---|---|
| `${springdoc.api-docs.path}` | `OpenApiWebMvcResource#openapiJson` | `/v3/api-docs` |
| `${springdoc.api-docs.path}.yaml` | `OpenApiWebMvcResource#openapiYaml` | `/v3/api-docs.yaml` |
| `${springdoc.api-docs.path}/{group}` | `MultipleOpenApiWebMvcResource` | `/v3/api-docs/{group}` |
| `${springdoc.api-docs.path}.yaml/{group}` | `MultipleOpenApiWebMvcResource` | `/v3/api-docs.yaml/{group}` |

plus `${springdoc.api-docs.path}/swagger-config` from `SwaggerConfigResource`. The `.yaml` variants
serve the *same complete specification*, and because `.yaml` is a suffix on the same path segment
the pattern `/v3/api-docs/**` does not match them — they need patterns of their own. So
`SecurityConfiguration` reads `springdoc.api-docs.path` and `springdoc.swagger-ui.path` back out of
the environment and builds all of these from them, rather than repeating literals that can drift
away from the endpoints they guard.

The actuator-hosted springdoc endpoints (`OpenApiActuatorResource`, `SwaggerWelcomeActuator`) are
never registered here: they require `springdoc.use-management-port=true` or `openapi`/`swagger-ui`
in `management.endpoints.web.exposure.include`, and `application.yaml` exposes only
`health,info,metrics`.

The closed state is *platform administrator*, not merely *authenticated*: `POST /api/v1/users`
is deliberately `permitAll` so learners can self-register, which means "any signed-in caller"
is not a confidentiality boundary on this platform. The check is
`DomainSecurityService#isPlatformAdmin()` — the `admin` domain held **globally**, not the
org-scoped `admin` domain an organisation administrator legitimately holds inside their own
organisation.

## Required variable

| Variable | Default | Meaning |
|---|---|---|
| `APP_API_DOCS_PUBLIC` | `false` | `true` serves the specification and the Swagger UI anonymously. Set it per environment; there is no environment where it is on implicitly. |

Bound at `app.api-docs.public` in `application.yaml` and read by
`shared/security/SecurityConfiguration`.

**Where the deploy reads it.** `docker/compose.yaml` declares `env_file: - .env` and enumerates
only `SERVER_PORT` under `environment:`, so every key in the deployment's `.env` reaches the
container — no per-variable passthrough entry is needed, and adding the key to `docker/.env.sample`
(done) is what makes it visible to whoever fills that file in. The extended compose file in
`install.md` lists variables explicitly instead, so it carries an explicit
`APP_API_DOCS_PUBLIC: ${APP_API_DOCS_PUBLIC:-false}` line.

It is **not** keyed on the Spring profile, and must not be changed to be. `docker/.env.sample`
ships `SPRING_PROFILES_ACTIVE=dev` and `docker/compose.yaml` takes the profile from that `.env`
(commit `39575e65` removed the compose-level default precisely so the deployment supplies it),
so a deployed server can perfectly well be running the `dev` profile. A gate that trusted the
profile would be no gate at all there.

## Per-environment settings

- **Production / staging**: leave `APP_API_DOCS_PUBLIC` unset or `false`. Nothing in the
  frontend calls these paths at runtime — the generated client in `services/client` is
  checked in, so no browser or server-rendered page fetches the spec.
- **Local development**: put `APP_API_DOCS_PUBLIC=true` in your `.env` (see
  `docker/.env.sample`) to browse `http://localhost:8080/swagger-ui/index.html` without first
  minting a Keycloak token. The dashboard's dev-only "API Docs" menu entry in the frontend
  points there.

Outside an opted-in environment the Swagger **UI** is unusable rather than merely protected:
the service is a bearer-only OAuth2 resource server with no login redirect, so a browser
hitting `/swagger-ui/index.html` gets a bare 401. That is intended — the UI is a local tool.

## Regenerating the frontend client

`elimika-ui`'s `openapi-ts.config.ts` fetches the spec with only an `accept` header, so it
cannot read a closed spec directly. It already supports being pointed at a file, which is the
supported route:

```bash
# Platform-admin access token from Keycloak
TOKEN=...

curl -sS -H "Authorization: Bearer $TOKEN" \
     -H "Accept: application/json" \
     https://api.elimika.staging.sarafrika.com/v3/api-docs > /tmp/api-docs.json

OPENAPI_SPEC_FILE=/tmp/api-docs.json \
API_BASE_URL=https://api.elimika.staging.sarafrika.com pnpm openapi-ts
```

The alternative — flipping `APP_API_DOCS_PUBLIC=true` on staging for the duration of a
regeneration — is available and is why the flag is an environment variable rather than a
constant, but it opens the whole spec to the internet while it is on. Prefer the token.

## Verifying a deployment

```bash
# Expect 401 (anonymous) — the spec is closed. Check every alias, not just the JSON one:
# these four are separate springdoc handler mappings serving the same specification.
for p in /v3/api-docs /v3/api-docs.yaml /v3/api-docs/springdocDefault /v3/api-docs.yaml/springdocDefault; do
  printf '%s -> ' "$p"
  curl -o /dev/null -s -w '%{http_code}\n' "https://<host>$p"
done

# Expect 403 with a non-admin token, 200 with a platform-admin token
curl -o /dev/null -s -w '%{http_code}\n' -H "Authorization: Bearer $TOKEN" https://<host>/v3/api-docs

# Expect 200 — the healthcheck must stay anonymous or the container never turns healthy
curl -o /dev/null -s -w '%{http_code}\n' https://<host>/actuator/health

# Expect 401 — operational detail is no longer anonymous
curl -o /dev/null -s -w '%{http_code}\n' https://<host>/actuator/metrics
```

If `/actuator/health` returns anything but 200 to an anonymous caller, the deployment will
flap: `docker/compose.yaml`'s healthcheck has no credentials.

## How the gate is decided

The `dev` profile serves the specification anonymously; `prod` restricts it to platform
administrators. This mirrors the commerce module, where `DevCommerceAccessServiceImpl` is
`@Profile("dev")` and relaxes the paywall while `CommerceAccessServiceImpl` is `@Profile("!dev")`
and enforces it — staging is the relaxed environment by design, and the frontend's generated client
is regenerated from staging's spec.

`APP_API_DOCS_PUBLIC` remains as a per-host override and takes precedence over the profile, so it
is deliberately **not** shipped in `docker/.env.sample` — an explicit value there would silence the
profile default on every host provisioned from it.
