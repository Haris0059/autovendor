# AutoVendor Backend — Java/Spring Implementation Plan

## Context

The frontend is feature-complete on a mock layer. The backend at `localhost:8000` is under construction — steps 1–3 of the implementation order are done (see **Status** below).

The backend is written in **Java + Spring**. This document is the implementation reference. Once built, the backend must replace the frontend mock layer (`USE_MOCKS=false`), the WordPress plugin will push webhooks into it, and it will drive OLX.ba on behalf of multiple user accounts.

The goal is a **unidirectional WooCommerce → OLX sync engine** with per-user multi-account support, scheduled + webhook-triggered syncs, encrypted credential storage, and a REST API that matches the contract the frontend already calls.

## Status (updated 2026-07-07)

Done so far — each slice ships with MockMvc + Testcontainers integration tests (22 passing):

1. ✅ **Bootstrap** — Maven, Spring Boot 4.0.6, Flyway, Testcontainers setup. *Leftovers:* no `Dockerfile`, no `backend` service in root `docker-compose.yml`, no actuator/healthcheck, no `dev`/`prod` profiles, virtual threads not enabled.
2. ✅ **Auth** (commit `4861884`) — `POST /auth/register` (201), `POST /auth/login`, `GET /auth/me`; BCrypt; `JwtAuthenticationFilter` + `SecurityConfig` (stateless, CORS for `http://localhost:3000`, JSON 401 entry point `{"detail": "Not authenticated"}`).
3. ✅ **OLX accounts** (commit `a968cff`) — `olx_accounts` (V2), AES-GCM `EncryptionService` + `EncryptedStringConverter`, `/olx/accounts` CRUD scoped per user, `OlxApiClient.login` called on create and on credential update; verified once against the real OLX API.
4. ✅ **OLX proxy basics** — `OlxApiClient` catalog methods + `authGet` (throws `OlxAuthException` on 401/403), `OlxTokenManager` (5-min refresh skew, one-retry-on-rejection via `withAccountToken`), `/olx/categories*` + `/locations/*` proxies with Redis caching (`olx-categories` 24h, `olx-locations` 7d). Key findings: OLX catalog/location endpoints are **public** (no Bearer needed); responses wrapped in `{"data": ...}`; `/cities` is a state→canton→city tree (cantons exist for RS/Brčko too, as regions); attribute `options` is `List<String>`; cached values must be `ArrayList`s (the Redis JSON serializer can't reconstruct JDK immutable lists).

**Next: step 5** — OLX listings: CRUD + status transitions, image upload, refresh (first real consumer of `OlxTokenManager.withAccountToken`).

---

## Tech stack

| Concern | Choice |
|---|---|
| Language | **Java 25 (LTS)** |
| Framework | **Spring Boot 4.0.6** (Boot 4 split many starters — see gotchas below) |
| Build | **Maven** |
| Web | spring-boot-starter-**webmvc** (Boot 4 name; Tomcat. Virtual threads not enabled yet — enable via `spring.threads.virtual.enabled: true` when jobs land) |
| Persistence | spring-boot-starter-data-jpa + Hibernate, PostgreSQL 16 driver |
| Migrations | **Flyway** (`V1__create_users.sql`, `V2__create_olx_accounts.sql`, …) |
| Auth | spring-boot-starter-security + JWT (HS256 via `io.jsonwebtoken:jjwt` 0.12.6) |
| HTTP client (OLX, Woo plugin) | Spring `RestClient` — in Boot 4 this needs the separate **spring-boot-starter-restclient** (already in pom) |
| Cache | spring-boot-starter-data-redis (categories, locations, token cache) |
| Jobs | **Spring `@Scheduled`** in-process; add ShedLock-Postgres only if scaling out |
| Crypto | **AES-GCM via JCA**, 256-bit key from `app.crypto.key` (base64; dev default in yml, override with `APP_CRYPTO_KEY` env via relaxed binding) |
| Validation | spring-boot-starter-validation (Jakarta Bean Validation) |
| JSON | **Jackson 3** (`tools.jackson`, Boot 4 default) — global `SNAKE_CASE` property naming in `application.yml` |
| Test | Boot 4 split test starters (webmvc-test, security-test, …) + Testcontainers (Postgres, Redis); `@MockitoBean` (not the removed `@MockBean`) for external clients |
| Observability | spring-boot-starter-actuator *(not added yet)* |
| Containerization | Multi-stage Dockerfile (eclipse-temurin:25-jdk → -jre) *(not created yet)*; reuse repo `docker-compose.yml` |

Postgres + Redis run via the repo's root `docker-compose.yml` (Postgres on host port **5433**, db `autovendor`). `application.yml` points at them directly; any value can be overridden per-environment through Spring's relaxed env binding (e.g. `SPRING_DATASOURCE_URL`, `APP_JWT_SECRET`, `APP_CRYPTO_KEY`). The root `.env.example` is a stale FastAPI-era leftover and should be rewritten or deleted.

**Boot 4 gotchas hit so far:** `@AutoConfigureMockMvc` moved to `org.springframework.boot.webmvc.test.autoconfigure`; no Jackson-2 `ObjectMapper` bean exists (use JsonPath in tests); `RestClient.Builder` requires `spring-boot-starter-restclient`.

---

## Module / package layout

Single Maven module, package root `ba.autovendor.backend` (groupId `ba.autovendor`, artifactId `backend`). ✅ = implemented, 🔶 = partial:

```
backend/
├── pom.xml                  ✅
├── Dockerfile               (todo)
└── src/main/java/ba/autovendor/backend/
    ├── BackendApplication.java  ✅
    ├── config/              ✅ SecurityConfig, JwtAuthenticationFilter, CacheConfig (todo: AsyncConfig if needed)
    ├── auth/                ✅ AuthController, AuthService, JwtService, dto/
    ├── user/                ✅ User entity, UserRepository, UserMapper, dto/
    ├── crypto/              ✅ EncryptionService (AES-GCM), EncryptedStringConverter (JPA converter)
    ├── olx/
    │   ├── account/         ✅ OlxAccount entity, OlxAccountController/Service/Repository/Mapper, dto/
    │   ├── client/          🔶 OlxApiClient (login + catalog + authGet), OlxTokenManager, dto/; todo: listing ops
    │   ├── listing/         ListingController, ListingService, dto/
    │   ├── category/        ✅ CategoryController, CategoryService (Redis-cached, 24h), CategoryMapper, dto/
    │   ├── location/        ✅ LocationController, LocationService (Redis-cached, 7d), dto/
    │   ├── sponsor/         SponsorController, DiscountController
    │   └── limit/           LimitController
    ├── woo/
    │   ├── store/           WooStore entity, StoreController, StoreService
    │   ├── client/          WooPluginClient (RestClient, X-AutoVendor-API-Key)
    │   └── product/         ProductController (read-through to plugin)
    ├── sync/
    │   ├── ProductLink entity, CategoryMapping entity, SyncLog entity
    │   ├── SyncController, MappingController, LinkController
    │   ├── SyncEngine        (orchestrates Woo → OLX)
    │   ├── HashComparator
    │   └── ImagePipeline     (download from Woo, upload to OLX)
    ├── webhook/             WebhookController (receives plugin events)
    ├── analytics/           AnalyticsController, AnalyticsService
    ├── jobs/                FullSyncJob, StockSyncJob, TokenRefreshJob
    └── common/              🔶 ApiError ✅, GlobalExceptionHandler ✅, OLX exceptions ✅; todo: PageResponse<T>
```

Conventions in the code so far: DTOs are Java records; mappers are static utility classes (`UserMapper.toResponse`); constructor injection, no Lombok on services (entities use `@Getter`); Jackson global snake_case means camelCase record fields serialize as `snake_case` automatically.

`db/migration/` for Flyway SQL.

---

## Data model (Flyway, one migration per slice)

- ✅ **users** (`V1__create_users.sql`): `id`, `email UNIQUE`, `password` (BCrypt hash — column is named `password`, not `password_hash`), `name`, `created_at`
- ✅ **olx_accounts** (`V2__create_olx_accounts.sql`): `id`, `user_id FK`, `username`, `encrypted_password BYTEA`, `olx_user_id`, `default_city_id`, `token_ciphertext BYTEA`, `token_expires_at`, `created_at`, **UNIQUE `(user_id, username)`**
- **woo_stores**: `id`, `user_id FK`, `name`, `store_url`, `encrypted_api_key BYTEA`, `created_at`
- **category_mappings**: `id`, `user_id FK`, `woo_category_id`, `woo_category_name`, `olx_category_id`, `olx_category_name`, unique `(user_id, woo_category_id)`
- **product_links**: `id`, `olx_account_id FK`, `woo_store_id FK`, `olx_listing_id`, `woo_product_id`, `sync_direction` (enum: `woo_to_olx`), `woo_hash`, `last_synced_at`, unique `(woo_store_id, woo_product_id)`
- **sync_logs**: `id`, `product_link_id FK NULL`, `action`, `status` (success/failed/skipped/pending), `message`, `created_at`, index on `(product_link_id, created_at DESC)`

All FKs `ON DELETE CASCADE` for user-owned rows. Booleans for soft state, JSONB only if needed later.

---

## Authentication ✅ (step 2 — done)

- BCrypt password hash on register (password min 8 chars — frontend Zod form still says 6, bump it in a later frontend PR).
- JWT HS256, 7-day expiry (`app.jwt.expiration-hours: 168`). Claims: `sub` = user id, `email`, `iat`, `exp`. Secret from `app.jwt.secret` (dev default in yml; override with `APP_JWT_SECRET` env).
- Response shape (matches frontend): `{ access_token, token_type: "Bearer", user: { id, email, name } }`. Register returns **201**.
- `JwtAuthenticationFilter` (in `config/`) parses the bearer token, loads the `User` entity into `SecurityContext`; invalid tokens fall through to the entry point, which returns 401 JSON `{"detail": "Not authenticated"}`. CSRF disabled, session policy `STATELESS`.
- CORS: allow `http://localhost:3000` (in `SecurityConfig`; make configurable for prod later).
- `@AuthenticationPrincipal User currentUser` injected into controllers (principal is the JPA `User` entity).
- Login returns the same 401 "Invalid credentials" for unknown email and wrong password (no user enumeration).

---

## Account scoping

The frontend currently passes `account_id` as a query/body field. The backend will enforce scoping by **path variable + ownership check**:

- Listings: `/olx/accounts/{accountId}/listings...`
- Sponsored, discounts, limits, listing-stats: same `{accountId}` prefix.
- Ownership checks: the accounts CRUD (done) uses **repository-scoped queries** (`findByIdAndUserId`) and returns **404** for other users' resources — this hides existence and needs no aspect. Keep the same pattern for nested resources: resolve `{accountId}` through `findByIdAndUserId` at the top of the service method (404 if not owned) instead of a separate `@AccountOwned` aspect, unless duplication grows.
- WooCommerce store scoping mirrors this: `/woo/stores/{storeId}/...`.

> **Frontend follow-up**: the existing hooks in `frontend/src/hooks/` (`use-listings`, `use-listing-limits`, `use-listing-stats`, `use-sponsored`, `use-woo-stores`'s nested resources) will need to switch from query-param scoping to path-param scoping. This is a mechanical refactor — track it as a separate PR after the backend is up.

---

## REST endpoints (final contract)

Grouped to mirror frontend hooks. All under `Authorization: Bearer <jwt>` unless noted. Response envelope = raw object or `PageResponse<T> { data, total, page, perPage, lastPage }`.

### Auth (`/auth`, public) ✅
- `POST /auth/register` — `{email, password, name}` (all required) → 201, token + user
- `POST /auth/login` — `{email, password}` → token + user
- `GET  /auth/me` — current user

### OLX accounts (`/olx/accounts`) ✅
- `GET    /olx/accounts` — ordered by `created_at DESC`
- `GET    /olx/accounts/{accountId}`
- `POST   /olx/accounts` — `{username, password, default_city_id?}` → 201; calls OLX `/auth/login` synchronously (invalid credentials → 400, nothing saved), stores encrypted password + token, `olx_user_id`, `token_expires_at = now + app.olx.token-ttl-days`
- `PUT    /olx/accounts/{accountId}` — re-logs-in to OLX only when username/password change (username-only change decrypts the stored password); `default_city_id`-only updates never call OLX
- `DELETE /olx/accounts/{accountId}` — 204

### OLX listings (`/olx/accounts/{accountId}/listings`)
- `GET    .` — `?status&page&per_page`
- `GET    ./all`
- `GET    /olx/listings/{id}`  *(global lookup; service still checks ownership via account)*
- `POST   .` — create draft
- `PUT    /olx/listings/{id}`
- `DELETE /olx/listings/{id}`
- `POST   /olx/listings/{id}/publish | /finish | /hide | /unhide`
- `PUT    /olx/listings/{id}/refresh`

### OLX categories (`/olx/categories`, Redis-cached 24h) ✅
- `GET /olx/categories` — `{id, name, slug, parent_id}[]`
- `GET /olx/categories/{parentId}` — children, same shape
- `GET /olx/categories/{id}/attributes` — `{type, name, input_type, display_name, options: string[], required}[]`
- `GET /olx/categories/{id}/brands`
- `GET /olx/categories/{id}/brands/{brandId}/models`

### Locations (`/locations`, Redis-cached 7d, behind our JWT like everything else) ✅
- `GET /locations/countries` — `{id, name, code}[]` (passthrough from OLX `/countries`)
- `GET /locations/states` — `{id, name, code}[]` (from OLX `/country-states`)
- `GET /locations/cantons` — `{id, name, state_id}[]` (flattened from `/country-states`; no standalone OLX endpoint)
- `GET /locations/cities` — `{id, name, zip_code: null, latitude, longitude, canton_id, state_id}[]` (flattened from the `/cities` tree; bulk zip codes aren't available upstream and the frontend doesn't display them)

### Sponsoring & discounts (`/olx/accounts/{accountId}`)
- `GET    .../sponsored`
- `GET    .../discounts`
- `GET    .../listings/{id}/sponsored/price` — `?type&days&refresh_every&locations`
- `POST   .../listings/{id}/sponsored`
- `DELETE .../sponsored/{id}`
- `POST   .../listings/{id}/discount`
- `POST   .../discounts/{id}/finish`

### Limits (`/olx/accounts/{accountId}`)
- `GET .../listing-limits`
- `GET .../listing/refresh/limits`

### WooCommerce stores (`/woo/stores`)
- `GET    /woo/stores`
- `GET    /woo/stores/{storeId}`
- `POST   /woo/stores` — `{name, store_url, api_key}`
- `PUT    /woo/stores/{storeId}`
- `DELETE /woo/stores/{storeId}`
- `POST   /woo/stores/test` — `{store_url, api_key}` → `{ok, products_count}`
- `GET    /woo/stores/{storeId}/products`
- `GET    /woo/stores/{storeId}/categories`
- `GET    /woo/stores/{storeId}/attributes`

### Sync (`/sync`)
- `GET    /sync/links`
- `POST   /sync/links`
- `DELETE /sync/links/{id}`
- `GET    /sync/mappings`
- `POST   /sync/mappings`
- `DELETE /sync/mappings/{id}`
- `GET    /sync/history` — paginated `?account_id&store_id&status&page&per_page`
- `POST   /sync` — `{product_link_id}` triggers immediate sync

### Webhook (`/webhook`, public, validated by `X-AutoVendor-Key`)
- `POST /webhook` — `{event, product_id, site_url, timestamp}` from the WP plugin

### Analytics (`/analytics`)
- `GET /analytics/listings?account_id=`
- `GET /analytics/refresh?account_id=`
- `GET /analytics/sponsors?account_id=`

Reference for exact request/response shapes: the frontend hooks under `frontend/src/hooks/` and the type files in `frontend/src/types/`. Mock implementations under `frontend/src/lib/mocks/` show concrete example payloads — treat those as fixtures, not contracts.

---

## External integrations

### OLX.ba (`https://api.olx.ba`)
- Login ✅: `POST /auth/login` `{username, password, device_name}` → `{token, user}`. Token is a Laravel-Sanctum-style Bearer string. Base URL, device name (`AutoVendor`) and token TTL are config (`app.olx.*`).
- **OLX does not document token lifetime** — we assume `app.olx.token-ttl-days` (30) and set `token_expires_at = login + TTL`. Treat it as a guess: `OlxTokenManager` must also re-login lazily when OLX rejects a token (remember: OLX returns **403/404, not 401**, for bad auth).
- All subsequent calls: `Authorization: Bearer <token>`.
- `OlxTokenManager` ✅ — decrypts the stored password (via the JPA converter), re-logs-in when `token_expires_at` is within 5 min, persists the new token (encrypted) + expiry; `withAccountToken(account, call)` retries exactly once when the call throws `OlxAuthException` (thrown by `OlxApiClient.authGet` on 401/403), then lets it propagate. First real consumer: listings (step 5).
- Catalog/location endpoints turned out to be **public** — proxied without a token.
- Listing CRUD, image upload (multipart `images[]` or `image_url`), set main image, publish/finish/hide/unhide/refresh, sponsor + discount, categories, locations, limits — all proxied through `OlxApiClient`.
- Rate limits not documented → conservative: retry once on 429 with jitter; surface failures into `sync_logs`.

### WordPress plugin (`/wp-json/autovendor/v1` or `/wp-json/integracija/v1`)
- Auth: `X-AutoVendor-API-Key: <stored key>` (also accept `X-Integracija-API-Key` so the alternate plugin works without changes).
- Reads: `/catalog`, `/catalog-hashes` (SHA-256 per product), `/catalog-stock`, `/product/{id}`, `/categories`, `/attributes`.
- Webhooks emitted by the plugin: `product.created`, `product.updated`, `product.deleted` → POSTed to whatever URL is configured under `autovendor_webhook_url` in WP admin. Backend exposes `POST /webhook` to receive them.
- `WooPluginClient` should fall back through both header names so either plugin variant works.

---

## Sync engine

**Full sync (scheduled, every 30 min):**
1. For each `woo_store`: GET `/catalog-hashes` (paginated).
2. Compare hashes against `product_links.woo_hash` → produce changed/new ID set.
3. Bulk GET `/catalog?ids=...` for changed products.
4. For each product:
   - If `product_link.olx_listing_id` exists → `PUT /listings/{id}` on OLX.
   - Else → `POST /listings` (draft), `POST /listings/{id}/image-upload` for each image, `POST /listings/{id}/image-main` for the first, `POST /listings/{id}/publish`.
5. Update `product_links.woo_hash`, `last_synced_at`. Write `sync_logs` row per product.

**Stock sync (scheduled, every 10 min):**
- Lighter: GET `/catalog-stock`, update only when stock or stock-status changed. `PUT /listings/{id}` with the minimal payload.

**Webhook sync (on demand):**
- Find `woo_store` by `site_url` from payload. Re-fetch single product. Resolve/create `product_link`. Same per-product flow as full sync.

**Token refresh (scheduled, hourly):**
- Iterate `olx_accounts`, re-login any with `token_expires_at < now() + 30 min`.

**Image pipeline:**
- `ImagePipeline.transfer(wooProduct, olxListingId)`: stream Woo image URL through `RestClient` → upload as multipart to OLX `/listings/{id}/image-upload`. Don't buffer entire files in memory; use `Resource` streaming.

**Category mapping:**
- Woo product → look up `category_mappings` by user + woo_category_id → resolve OLX `category_id`. If unmapped, sync skipped with `status='skipped'`, `message='no category mapping'`.

---

## Crypto ✅ (done in step 3)

`EncryptionService` (`crypto/`):
- `byte[] encrypt(String plaintext)` → returns IV(12) + ciphertext + tag, AES-256-GCM.
- `String decrypt(byte[])`.
- Key loaded once from `app.crypto.key` (base64-encoded 32 bytes; dev default in `application.yml`, override with `APP_CRYPTO_KEY` env in prod — **generate a fresh prod key**, the dev key is committed).
- Used by: `OlxAccount.encrypted_password`, `OlxAccount.token_ciphertext` ✅; `WooStore.encrypted_api_key` (todo).
- `EncryptedStringConverter` — JPA `AttributeConverter<String, byte[]>`, a Spring `@Component` (Boot registers `SpringBeanContainer`, so Hibernate injects it); entities expose `String`, columns store `BYTEA`.
- ⚠️ Changing the key makes existing rows undecryptable — there is no key-rotation support; if rotation is ever needed, add a re-encrypt migration path first.

---

## Error handling

`GlobalExceptionHandler` (`@RestControllerAdvice`) maps:
- ✅ `MethodArgumentNotValidException` → 400 `{detail: "Validation failed", errors[]}`
- ✅ `BadCredentialsException`, `JwtException` → 401 `{detail}`
- ✅ `AccessDeniedException` → 403 `{detail}` (currently unused — ownership failures surface as 404 via scoped queries)
- ✅ `EntityNotFoundException` → 404 `{detail}`
- ✅ `IllegalArgumentException` → 400 `{detail}` (duplicate email / duplicate OLX account, etc.)
- ✅ `InvalidOlxCredentialsException` → 400 `{detail: "Invalid OLX credentials"}`
- ✅ `OlxApiException` → 502 `{detail}`; `WooPluginException` → 502 (todo with the Woo slice)
- ✅ Everything else → 500 with sanitized message.
- ✅ Unauthenticated requests (no/bad JWT) → 401 `{detail: "Not authenticated"}` from the security entry point, not the advice.

Response shape `{detail: string}` matches what the frontend's `api-client.ts` already parses.

---

## Critical files to be modified or created

**New (backend):**
- ✅ `backend/pom.xml`
- `backend/Dockerfile` (todo)
- 🔶 `backend/src/main/resources/application.yml` — exists; `dev`/`prod` profiles still todo
- ✅ `db/migration/V1__create_users.sql`, `V2__create_olx_accounts.sql`; further `V3+` per slice
- 🔶 Java sources under `backend/src/main/java/ba/autovendor/backend/...` (see layout above for per-package status)

**Modified:**
- `docker-compose.yml` (root) — add the `backend` service alongside postgres/redis (todo).
- Root `.env.example` — stale FastAPI-era file; rewrite for the Spring env vars or delete (todo).
- `frontend/.env.local` — set `NEXT_PUBLIC_USE_MOCKS=false` once endpoints are live; ensure `NEXT_PUBLIC_API_URL` points to backend.
- `frontend/src/components/signup-form.tsx` — bump password Zod min from 6 to 8 to match the backend (todo, frontend PR).
- `frontend/src/hooks/use-listings.ts`, `use-sponsored.ts`, `use-listing-limits.ts`, `use-listing-stats.ts`, `use-woo-stores.ts` — migrate from query/body `account_id` / `store_id` to path-based scoping (separate PR after backend live).

**Read-only references during implementation:**
- `frontend/src/lib/api-client.ts` — auth header convention, error envelope, multipart helper.
- `frontend/src/types/` — exact response shapes the frontend expects.
- `frontend/src/lib/mocks/*` — example payloads.
- `wp-plguin/autovendor.php`, `wp-plguin/integracija-v3.php` — request/response of plugin endpoints; webhook payload shape.
- `olx-api-docs/` — all OLX.ba endpoint shapes, especially listings, image-upload, sponsor, categories, locations.

---

## Implementation order (suggested for manual coding)

1. ✅ **Bootstrap**: `pom.xml`, `application.yml`, Flyway baseline. *(Deferred leftovers: `backend` service in docker-compose, Dockerfile, actuator healthcheck — fold into a later step.)*
2. ✅ **Auth**: User entity, register, login, `GET /auth/me`, JWT filter, Spring Security config. (commit `4861884`)
3. ✅ **OLX accounts**: entity, encryption, CRUD, OLX login on create + credential update. (commit `a968cff`)
4. ✅ **OLX proxy basics**: `OlxApiClient` + `OlxTokenManager`, categories + locations with Redis caching.
5. **OLX listings**: CRUD + status transitions, image upload, refresh.
6. **WooCommerce**: store CRUD, `WooPluginClient`, connect-test endpoint, products/categories/attributes proxy.
7. **Sync foundations**: `product_links`, `category_mappings`, `sync_logs`, manual `POST /sync`.
8. **Sync engine**: full sync flow, hash comparison, image pipeline.
9. **Webhook receiver** + webhook-triggered sync path.
10. **Background jobs**: `@Scheduled` full sync, stock sync, token refresh.
11. **Sponsored + discounts + limits**.
12. **Analytics endpoints** (initially can be derived from `sync_logs` + listings counts).
13. **Frontend cutover**: flip `USE_MOCKS=false`, update hooks for path-based scoping.

Each step is independently testable end-to-end against the frontend (run frontend with `USE_MOCKS=false` and point it at the partial backend; un-implemented endpoints will 404 but the implemented pages will work).

---

## Verification

- **Per-endpoint**: hit each route with `curl` or Bruno/Insomnia matching the request shapes documented above; compare JSON to the corresponding frontend type.
- **Integration test per slice**: Testcontainers (Postgres + Redis) + MockMvc; external clients replaced with `@MockitoBean` (done for `OlxApiClient` in the accounts slice). For the sync engine, where request-building/parsing matters, add WireMock (or `MockRestServiceServer`) to stand in for `api.olx.ba` and the WP plugin.
- **End-to-end smoke**: `docker compose up` (postgres + redis + backend), `npm run dev` in `frontend/` with `NEXT_PUBLIC_USE_MOCKS=false`. Walk through:
  - Register → login → land on dashboard. *(✅ verified via curl; UI walkthrough pending mock cutover)*
  - Add an OLX account (real credentials) → see it appear, token persisted. *(✅ verified via curl with the real OLX API; credentials stored as BYTEA ciphertext)*
  - Add a WooCommerce store → `POST /woo/stores/test` returns `ok:true`.
  - Map at least one Woo category → OLX category.
  - Trigger `POST /sync` for one product_link → see it appear as a draft on OLX, then auto-publish, with a `sync_logs` row written.
  - Modify the product in WooCommerce → webhook fires → backend updates the OLX listing.
  - Wait 30 min (or trigger the job manually via an actuator endpoint) → confirm full-sync picks up further changes.
- **Security**: confirm `404` when user A tries to fetch user B's account/store/listing by ID (scoped queries hide existence — covered by tests for accounts ✅); confirm credentials in the DB are encrypted (`SELECT encrypted_password FROM olx_accounts` shows BYTEA, not plaintext ✅ verified).
