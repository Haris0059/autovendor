# AutoVendor Backend — Java/Spring Implementation Plan

## Context

The frontend is feature-complete on a mock layer. The backend at `localhost:8000` is under construction — steps 1–3 of the implementation order are done (see **Status** below).

The backend is written in **Java + Spring**. This document is the implementation reference. Once built, the backend must replace the frontend mock layer (`USE_MOCKS=false`), the WordPress plugin will push webhooks into it, and it will drive OLX.ba on behalf of multiple user accounts.

The goal is a **unidirectional WooCommerce → OLX sync engine** with per-user multi-account support, scheduled + webhook-triggered syncs, encrypted credential storage, and a REST API that matches the contract the frontend already calls.

## Status (updated 2026-07-12)

Done so far — each slice ships with MockMvc + Testcontainers integration tests (176 passing):

1. ✅ **Bootstrap** — Maven, Spring Boot 4.0.6, Flyway, Testcontainers setup. *Leftovers:* no `Dockerfile`, no `backend` service in root `docker-compose.yml`, no actuator/healthcheck, no `dev`/`prod` profiles, virtual threads not enabled.
2. ✅ **Auth** (commit `4861884`) — `POST /auth/register` (201), `POST /auth/login`, `GET /auth/me`; BCrypt; `JwtAuthenticationFilter` + `SecurityConfig` (stateless, CORS for `http://localhost:3000`, JSON 401 entry point `{"detail": "Not authenticated"}`).
3. ✅ **OLX accounts** (commit `a968cff`) — `olx_accounts` (V2), AES-GCM `EncryptionService` + `EncryptedStringConverter`, `/olx/accounts` CRUD scoped per user, `OlxApiClient.login` called on create and on credential update; verified once against the real OLX API.
4. ✅ **OLX proxy basics** (commit `5851c04`) — `OlxApiClient` catalog methods + `authGet` (throws `OlxAuthException` on 401/403), `OlxTokenManager` (5-min refresh skew, one-retry-on-rejection via `withAccountToken`), `/olx/categories*` + `/locations/*` proxies with Redis caching (`olx-categories` 24h, `olx-locations` 7d). Key findings: OLX catalog/location endpoints are **public** (no Bearer needed); responses wrapped in `{"data": ...}`; `/cities` is a state→canton→city tree (cantons exist for RS/Brčko too, as regions); attribute `options` is `List<String>`; cached values must be `ArrayList`s (the Redis JSON serializer can't reconstruct JDK immutable lists).
5. ✅ **OLX listings** (commits `bf9e915` backend, `54bf9f1` frontend; follow-ups `f5ce642`, `10a7cd0`) — **path-scoped** `/olx/accounts/{accountId}/listings...` (CRUD, publish/finish/hide/unhide, refresh, multipart image upload/delete/main) + user-wide `GET /olx/listings/all` (all accounts × statuses, page cap 10/status, Redis-cached 5 min in `olx-listings-all`, evicted on mutations). Frontend `use-listings.ts` refactored to the path-scoped routes (mutations resolve the account internally; active account now persisted in localStorage). Verified live with a draft lifecycle on the real account. **OLX gotchas found (July 2026):**
   - ⚠️ `PUT /listings/{id}` on a **draft publishes it** (status flips to active) — there is no way back to draft.
   - There is **no endpoint to list drafts** (`/inactive` does NOT contain them; `/listings/draft(s)` etc. 404) — drafts are reachable only by id after creation. Dashboard stats therefore can't count drafts.
   - `/users/{id}/listings/hidden` returns items whose `status` field still says `active` → backend forces `status=hidden` on that route.
   - `city_id` is only persisted when `country_id` is sent along (BiH = **49**, not 1 — frontend form default fixed).
   - List envelope is `{data, meta:{total,last_page,current_page,per_page}}` (docs claimed top-level fields); `per_page` IS honored despite being undocumented.
   - Listing responses carry image **URLs only** (no image ids); ids exist only in the image-upload response — so existing remote images can't be deleted/re-mained after a refetch (frontend shows "not supported yet" for that).
   - Some list endpoints (e.g. `/finished`) return `images: null` with only the single `image` thumbnail set → mapper falls back to it (`f5ce642`).
   - List items have no `created_at` — only `date` (the refresh/bump timestamp), so "Kreirano" in the table shows the bump date; the accurate `created_at` exists only on single-listing GETs.

   **Frontend UX conventions from this slice:** the shared `DataTable` shows skeletons when fetching with no rows on screen and a dim + "Učitavanje…" overlay when refetching over existing rows — new tables should pass `isFetching` from their query; the listings list query uses `staleTime: 0` (overriding the global 60 s) so every tab visit revalidates visibly (`10a7cd0`). Selects built on Base UI need the `items` prop or the trigger renders the raw value (`eed58be`).

6. ✅ **WooCommerce** — `woo_stores` (V3, AES-GCM-encrypted `api_key`, UNIQUE `(user_id, store_url)`), `WooPluginClient` (**AutoVendor plugin variant only** — decision; header auth only, never the `api_key` query param, since that lands in WP access logs), store CRUD, connect tests, catalog proxy. Verified live against alpus.ba (13 products) + frontend walkthrough with `USE_MOCKS=false`.
   - Endpoints: `/woo/stores` CRUD; `POST /woo/stores/test` (body `{store_url, api_key}`) **and** `POST /woo/stores/{id}/test` (stored key — new, path-scoped; frontend detail page switched to it and the masked-key hack was removed); `GET /woo/stores/{id}/products|categories|attributes` (bare arrays per frontend contract).
   - Connect test = paging the plugin's lightweight `/catalog-hashes` (per_page 200, cap 50 pages) summing `count` → `{ok, products_count}`; also runs pre-persist on create and on url/key change in update (name-only updates never call the plugin). Store URLs normalized (default https, strip trailing `/`) before dedupe/persist.
   - Products proxy aggregates plugin `/catalog` pages (per_page 100, cap 100 pages) into frontend `WooProduct` shape: `stock_qty`→`stock_quantity`, `currency` hardcoded `"KM"` (plugin exposes none), product-embedded categories get `parent=0`; `/categories` flattens the plugin's nested tree (parent = enclosing node, no `count` upstream → null); attributes get `has_archives=false`, `variation=false`, `options=null` (frontend falls back to `terms`).
   - Redis caches `woo-products` (5 min) / `woo-categories` / `woo-attributes` (15 min), keyed by store id, evicted on store update/delete. ⚠️ Ownership-vs-cache trap: `@Cacheable` sits on a separate `WooCatalogFetcher` while `WooCatalogService` runs `findByIdAndUserId` on **every** call — a cache hit can never skip the ownership check (pinned by test).
   - Error mapping: plugin 401/403 → `InvalidWooApiKeyException` → 400 "Invalid WooCommerce API key"; WP `rest_no_route` 404 (plugin not installed) → 400 "AutoVendor plugin not found on this store"; other upstream 4xx → 400 with WP's `message`; 5xx/connectivity → 502 via `WooPluginException`.
   - **Boot 4 gotcha:** `ClientHttpRequestFactorySettings` was renamed — timeouts are now `ClientHttpRequestFactoryBuilder.detect().build(HttpClientSettings.defaults().withTimeouts(connect, read))` (`org.springframework.boot.http.client.HttpClientSettings`).
   - **WP plugin gotcha:** the settings page (API key) hangs off the WooCommerce admin menu (`WooCommerce → AutoVendor`) and silently disappears if WooCommerce is inactive; key lives in the `autovendor_api_key` option.
   - Sync-engine note (step 7+): the plugin swaps attribute semantics — `name` holds the label, `slug` the raw name.

7. ✅ **Sync foundations** — `product_links` / `category_mappings` / `sync_logs` (V4), `/sync/links` + `/sync/mappings` CRUD, manual `POST /sync` (full per-product Woo→OLX sync), `GET /sync/history` (Specification filters: status/account/store + pagination). Verified live end-to-end: alpus.ba product → mapping → link (null listing id) → create draft → images → publish → re-sync took the update path → cleanup. Frontend wired: "Poveži" on the store detail page opens a keyed `LinkProductDialog` with an optional listing id ("Novi artikal" sentinel → `null`).
   - **Transaction split**: no `@Transactional` across HTTP calls; `SyncEngine` never throws (returns `SyncOutcome`), the log row is persisted after; `POST /sync` returns **200 with the log even for failed/skipped** outcomes. `olx_listing_id` is saved immediately after create, *before* images/publish, so a partial failure retries down the update path (and the PUT-publishes-a-draft gotcha completes the publish).
   - `sync_logs` denormalizes `user_id`/`olx_account_id`/`woo_store_id` (filters need no JOIN); `product_link_id` is `ON DELETE SET NULL` — history survives link deletion (observed live; the frontend hides retry on such rows).
   - Enums `SyncDirection`/`SyncStatus` use deliberate **lowercase constants** so DB/`@Enumerated(STRING)`/JSON/query params align with zero Jackson config. All 3 directions are stored; non-`woo_to_olx` syncs log `skipped`.
   - **OLX gotchas found (July 2026):**
     - ⚠️ Create-listing **requires the `attributes` field to be present** (even `[]`) once `category_id` is sent — otherwise 422 `kategorija zahtjeva prisutno polje attributes`, including for categories with no required attributes. Engine always sends `attributes: []`; real attribute mapping is step 8.
     - ✅ **`image_url` upload format pinned**: JSON body `{"image_url": "<absolute url>"}` to `POST /listings/{id}/image-upload` works (OLX downloads the image itself; Woo-hosted URLs fine, first upload set as main via the existing main-image endpoint). No multipart fallback needed — `uploadImages` (multipart) remains for the frontend proxy only.
     - OLX validation errors arrive **nested**: `{"error": {"message", "errors": {field: msg}}}` — `OlxApiClient.olxError` unwraps that envelope and appends field errors so sync logs show the real reason (pinned by `OlxApiClientErrorTest`).
     - HTML descriptions render correctly on the public listing page (SSR escapes them in the page source, but the UI renders the markup) — descriptions are sent as-is, no tag stripping.
   - Payload whitelist (`SyncEngine.buildPayload`): title (truncated to 65), short_description (tags stripped), description (HTML as-is), price (fallback `regular_price`), `country_id` 49 + account's `default_city_id`, category via mapping (first Woo category; unmapped/no-category → `skipped` log naming the category), `sku_number`, `available` from stock_status, `listing_type` "sell", `state` "new", `attributes: []`.

8. ✅ **Scheduled bulk sync + webhook receiver** — automation on top of the step-7 engine; verified live against alpus.ba + the real OLX account (webhook simulation with the plugin's exact payload/headers, and a real 1-minute sweep run). No new migration.
   - **Sweep** (`BulkSyncService.syncStore`, `@Scheduled` via `SyncScheduler`, default every 10 min, config `app.sync.*`): page `/catalog-hashes` → re-sync links whose hash changed (null `woo_hash` = changed) → **auto-link** unlinked products, but only when the sync can fully proceed right now (publish status + exactly one OLX account on the user + first category mapped + default city set); blockers stay silent in the sweep (they'd repeat every run) and are logged once per event on the webhook path. Per-store try/catch isolates dead stores; programmatic `olx-listings-all` eviction per affected user.
   - **Log spam control**: failed/skipped sweep outcomes are not re-logged while the newest log for that link has identical action+status+message; successes always log. Manual `POST /sync` stays forced (no hash gate).
   - **Webhook** `POST /webhook` (permitAll): plugin sends `X-AutoVendor-Key` = the store's API key + `{event, product_id, site_url, timestamp}`. Auth = normalize `site_url` → all `woo_stores` with that URL → constant-time key compare (`MessageDigest.isEqual`) → **every** matching store processed (same URL can belong to several users — observed live: second tenant got its own skipped log). 401 when nothing matches; unknown events → 200 + warn (fire-and-forget caller). `product.updated`/`created` → hash-gate (absorbs WP's double-fires — verified live) then sync existing link or auto-link; `product.deleted` → `SyncEngine.hide` (listing hidden on OLX, link kept, `action="hide"` log).
   - **Token upkeep**: hourly `@Scheduled` re-login for accounts expiring within 30 min (`OlxTokenManager.refreshIfExpiringWithin`); lazy refresh in `withAccountToken` remains the fallback.
   - **Scheduling infra**: `@EnableScheduling` lives in `SchedulingConfig` behind `app.sync.scheduling-enabled` — tests disable it centrally via a `DynamicPropertyRegistrar` in `TestcontainersConfiguration` and call the service methods directly. Virtual threads enabled (`spring.threads.virtual.enabled: true`). Single-threaded scheduler = sweep runs never overlap.
   - **Default-city pre-check** on the engine's create path → `skipped` "OLX account has no default city set" before any OLX call.
   - Note: the single-listing `GET /listings/{id}` still reports `status: active` for hidden listings (same OLX quirk as the hidden-list endpoint) — verify hides via `?status=hidden`.
   - Frontend: history page gained the `skipped` filter/badge and the `hide` action label; the store detail "Webhook Logovi" tab now shows real per-store history via `GET /sync/history?store_id=`.
   - Deviation from the original sketch: **no separate stock job** — the plugin's product hash already covers stock/price/images/text, so the hash-gated sweep detects stock changes; a stock-only path would have needed a stored stock snapshot + an unverified partial-PUT for no extra coverage.

9. ✅ **Attribute mapping (defaults + auto-match)** — category mappings carry default values for the OLX category's attributes (`attribute_defaults` JSONB, V5); at sync time a product's own Woo attribute values override a default when they match an OLX option. Live-verified end-to-end (mapping created through the new dialog → product 3001 published into required-attribute category 1179 → public page shows "Vrsta: Za keramiku" → update path keeps attributes → cleanup).
   - ✅ **OLX `attributes` payload format pinned live (July 2026)**: array of `{"id": <attributeId>, "value": "<exact option string>"}` — works on create and update; OLX validates per element and errors are keyed by the attribute id (`{"3753": "Vrijednost X za polje Vrsta nije validno"}`). Wrong values 422 — so acceptance means persistence (also confirmed on the public page's "Osobine" section).
   - **Validation** (create + `PUT /sync/mappings/{id}`): via cached `CategoryService.getAttributes`, every `required` attribute needs a default (decided with Haris — mapped categories then can never fail OLX attribute validation), keys must exist, values must be in `options`. `AttributeResponse` now carries the attribute `id` (the engine needs it for the payload; ⚠️ old Redis cache entries lack it — flush `olx-categories` on deploy).
   - **Engine resolution** per category attribute: auto-match Woo product attribute (label/name vs display_name/name, value vs options, case/diacritic-insensitive with a manual đ→d map — đ has no NFD decomposition) → mapping default → required-with-no-value skips *before* any OLX call ("set a default on the category mapping"), optional omitted. Plugin per-product attrs now mapped (`WooProductAttributeDto`: `name` = taxonomy, `label` = human, `options` = values).
   - **Mapping dialog** got a 4-level cascading OLX category picker (top-level-only select was a dead end — attribute-bearing categories are leaves 3+ levels deep; the listing form still has the old 2-level picker, worth aligning later), renders one field per attribute (select for option-bearing, text otherwise), blocks save until required defaults are set, edit action (pencil) reuses the dialog with a `PUT`; defaults count badge in the table.

10. ✅ **Sponsored + discounts + limits** (step 11 of the implementation order) — OLX proxy for quotes/limits + **DB tracking** for the lists (V6: `sponsorships`, `discounts`); live-verified free ops only.
   - **Probe findings (July 2026)**: OLX has **no list endpoints** for sponsorships/discounts (`/sponsored`, `/discounts`, `/users/{uid}/sponsored` … all 404; GET on the `sponsore`/`discount` paths → 500). Listings *do* carry markers (`sponsored: 0|1`, `has_discount`, `discounted_price_float` on list items and single GETs) — no days/price/ends-at though, so rows created through AutoVendor are tracked in our DB; the markers are a future reconciliation hook. Sponsorships/discounts made directly on olx.ba stay invisible to us (documented limitation).
   - **Pinned live shapes**: `/listing-limits` deviates from the docs — `{"data":{"cars"|"real-estate"|"car-parts"|"other":{"limit","unlimited","listings"}}}` (hyphenated keys, extra `car-parts` category, `listings` not `used`); mapper is JsonNode-based, tolerant (missing keys → zeros + WARN), drops `car-parts`, ignores `unlimited`. `/listing/refresh/limits` is flat, no envelope, matches docs. **Sponsor price quote requires Laravel array notation `locations[]=homepage`** — plain `locations=` 422s with "Zona treba biti niz"; response is flat with extra fields (`total_without_discount`, `discount`, `discount_percentage`).
   - **Routes** (deviates from the earlier contract sketch): lists are **user-wide** (`GET /olx/sponsored`, `GET /olx/discounts` — the Sponzorstva page has no account picker; rows carry `account_id`), end-ops are **row-scoped** (`DELETE /olx/sponsored/{id}`, `POST /olx/discounts/{id}/finish` — the row knows its account), quote/create/limits are account-path-scoped (`/olx/accounts/{accountId}/listings/{listingId}/sponsored[/price]`, `…/discount`, `…/listing-limits`, `…/listing/refresh/limits`).
   - **Row lifecycle**: active = `ended_at IS NULL AND ends_at > now()`; natural expiry needs no job; create supersedes active rows for the same listing; ended rows are kept (history feeds step-12 analytics). Sponsor create quotes the price first (free) and stores `price_total` from the quote — the `sponsore` POST response is undocumented and only logged at INFO (never run live: charges credits).
   - ⚠️ **Sponsor cancel is a type-0 re-POST — UNVERIFIED live** (no delete endpoint documented; a real test would require paying for a sponsorship). Deliberately lenient: upstream 4xx still ends the row (never stuck in the UI), 5xx/connectivity keeps it. Discount finish is the opposite (documented + live-verified): 4xx propagates, row stays.
   - Validation (400 before any OLX call): sponsor `type ∈ {1,2}`, `days ∈ {1,2,3,5,7,14,21,30}`, `refresh_every ∈ {0,3,6,8,24}`, locations restricted to `[a-z0-9_-]+` (they go into the OLX query string unencoded); discount `days ∈ {3,7,30}`, prices > 0, discount < original.
   - **Live verification**: limits + quotes through the new endpoints matched the probe raw JSON; full discount cycle on real listing 77163754 (500 → 480 KM, `has_discount: true` + "480 KM" on the public API/page → finish → restored to 500, row left the active list). Hibernate gotcha: `SMALLINT` column vs `Integer` field fails schema validation — use `INT`.
   - Frontend: `use-sponsored.ts` mutations/quote moved to the account-scoped paths (via `useResolvedAccountId`, now exported from `use-listings.ts`); `use-listing-limits.ts` path-scoped; `MockSponsoredListing`/`MockDiscount` promoted to `OlxSponsorship`/`OlxDiscount`/`OlxSponsorPrice` in `types/olx.ts`; **sponsor-dialog offered OLX-invalid values** (days 15, refresh 1/12, default refresh "1") — fixed to the real OLX sets.

**Next: steps 12+** — Analytics endpoints (derivable from `sync_logs` + the new sponsorships/discounts tables + listing counts), frontend mock cutover (`USE_MOCKS=false` default). Also worth folding in: the listing form's category picker only goes 2 levels deep (mappings dialog now does 4) and bootstrap leftovers (Dockerfile, backend in docker-compose, actuator healthcheck).

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
    │   ├── client/          🔶 OlxApiClient (login, catalog, listing ops, multipart images), OlxTokenManager, dto/; todo: sponsor/discount/limits ops
    │   ├── listing/         ✅ ListingController (path-scoped), AllListingsController, ListingService, ListingMapper, dto/
    │   ├── category/        ✅ CategoryController, CategoryService (Redis-cached, 24h), CategoryMapper, dto/
    │   ├── location/        ✅ LocationController, LocationService (Redis-cached, 7d), dto/
    │   ├── sponsor/         ✅ Sponsorship/Discount entities+repos, Sponsor/DiscountController+Service, mappers, dto/
    │   └── limit/           ✅ LimitController, LimitService, LimitMapper (defensive JsonNode mapping), dto/
    ├── woo/
    │   ├── store/           ✅ WooStore entity, StoreController, StoreService, WooStoreRepository/Mapper, dto/
    │   ├── client/          ✅ WooPluginClient (RestClient, X-AutoVendor-API-Key), dto/
    │   └── product/         ✅ WooCatalogController, WooCatalogService (ownership) + WooCatalogFetcher (Redis-cached), WooCatalogMapper, dto/
    ├── sync/
    │   ├── ProductLink entity, CategoryMapping entity, SyncLog entity
    │   ├── SyncController, MappingController, LinkController
    │   ├── SyncEngine        (orchestrates Woo → OLX)
    │   ├── HashComparator
    │   └── ImagePipeline     (download from Woo, upload to OLX)
    ├── webhook/             WebhookController (receives plugin events)
    ├── analytics/           AnalyticsController, AnalyticsService
    ├── jobs/                FullSyncJob, StockSyncJob, TokenRefreshJob
    └── common/              ✅ ApiError, GlobalExceptionHandler, OLX exceptions, PageResponse<T>
```

Conventions in the code so far: DTOs are Java records; mappers are static utility classes (`UserMapper.toResponse`); constructor injection, no Lombok on services (entities use `@Getter`); Jackson global snake_case means camelCase record fields serialize as `snake_case` automatically.

`db/migration/` for Flyway SQL.

---

## Data model (Flyway, one migration per slice)

- ✅ **users** (`V1__create_users.sql`): `id`, `email UNIQUE`, `password` (BCrypt hash — column is named `password`, not `password_hash`), `name`, `created_at`
- ✅ **olx_accounts** (`V2__create_olx_accounts.sql`): `id`, `user_id FK`, `username`, `encrypted_password BYTEA`, `olx_user_id`, `default_city_id`, `token_ciphertext BYTEA`, `token_expires_at`, `created_at`, **UNIQUE `(user_id, username)`**
- ✅ **woo_stores** (`V3__create_woo_stores.sql`): `id`, `user_id FK`, `name`, `store_url`, `encrypted_api_key BYTEA`, `created_at`, **UNIQUE `(user_id, store_url)`**
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

> **Frontend follow-up**: ✅ `use-listings` switched to path-param scoping (mutations resolve the active account internally via `useActiveAccount`, persisted in localStorage, with first-account fallback). ✅ `use-sponsored` + `use-listing-limits` migrated in step 10. Still pending when its slice lands: `use-listing-stats` (analytics). ✅ `use-woo-stores` was already path-scoped; its detail-page test now uses `POST /woo/stores/{id}/test` (new `useTestWooStoreConnection`).

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

### OLX listings (`/olx/accounts/{accountId}/listings`) ✅
- `GET    .` — `?status&page&per_page` → `PageResponse`; status routes: active → `/users/{username}/listings`, others → `/users/{olxUserId}/listings/{status}`; `draft` aliases `inactive` (but see gotcha: OLX can't list drafts)
- `GET    /olx/listings/all` — user-wide aggregate (dashboard), Redis-cached 5 min, evicted on mutations
- `POST   .` — 201, creates OLX draft; payload whitelisted (frontend-only fields like `top_category_id` stripped)
- `GET/PUT/DELETE ./{listingId}` — ⚠️ PUT on a draft publishes it (OLX behavior)
- `POST   ./{listingId}/publish | /finish | /hide | /unhide`; `PUT ./{listingId}/refresh`
- `POST   ./{listingId}/images` (multipart `images`), `DELETE ./{listingId}/images/{imageId}`, `POST ./{listingId}/images/{imageId}/main`

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

### Sponsoring & discounts ✅ (lists user-wide, end-ops row-scoped — see step 10 notes)
- `GET    /olx/sponsored` — user-wide, active tracking rows
- `GET    /olx/discounts` — user-wide, active tracking rows
- `GET    /olx/accounts/{accountId}/listings/{id}/sponsored/price` — `?type&days&refresh_every&locations` (comma-joined; backend splits and sends `locations[]=` to OLX)
- `POST   /olx/accounts/{accountId}/listings/{id}/sponsored` → 201 tracking row
- `DELETE /olx/sponsored/{id}` → 204 (`id` = tracking-row id; type-0 cancel on OLX, unverified)
- `POST   /olx/accounts/{accountId}/listings/{id}/discount` → 201 tracking row
- `POST   /olx/discounts/{id}/finish` (`id` = tracking-row id)

### Limits ✅ (`/olx/accounts/{accountId}`)
- `GET .../listing-limits`
- `GET .../listing/refresh/limits`

### WooCommerce stores (`/woo/stores`)
- `GET    /woo/stores`
- `GET    /woo/stores/{storeId}`
- `POST   /woo/stores` — `{name, store_url, api_key}`
- `PUT    /woo/stores/{storeId}`
- `DELETE /woo/stores/{storeId}`
- `POST   /woo/stores/test` — `{store_url, api_key}` → `{ok, products_count}`
- `POST   /woo/stores/{storeId}/test` — same response, uses the stored key (added for the detail page)
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
- Used by: `OlxAccount.encrypted_password`, `OlxAccount.token_ciphertext` ✅; `WooStore.encrypted_api_key` ✅.
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
- ✅ `OlxApiException` → 502 `{detail}` (or 400 when upstream 4xx); `WooPluginException` → same mapping ✅; `InvalidWooApiKeyException` → 400 ✅
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
- ✅ `frontend/src/components/signup-form.tsx` — password Zod min bumped 6 → 8 to match the backend; `api-client.ts` now appends the backend's `errors[]` to error toasts.
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
5. ✅ **OLX listings**: path-scoped CRUD + status transitions, image upload, refresh; frontend hooks refactored to path scoping.
6. ✅ **WooCommerce**: store CRUD, `WooPluginClient`, connect-test endpoints, products/categories/attributes proxy; verified live against alpus.ba.
7. **Sync foundations**: `product_links`, `category_mappings`, `sync_logs`, manual `POST /sync`.
8. **Sync engine**: full sync flow, hash comparison, image pipeline.
9. **Webhook receiver** + webhook-triggered sync path.
10. **Background jobs**: `@Scheduled` full sync, stock sync, token refresh.
11. ✅ **Sponsored + discounts + limits**.
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
