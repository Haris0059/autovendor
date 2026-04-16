# AutoVendor Frontend Build — Progress & Handoff

**Branch:** `feat/frontend-buildout`
**Plan:** `/home/haris/.claude/plans/vivid-stirring-patterson.md`
**Last updated:** 2026-04-16

---

## Where I stopped

End of **Phase C — Listings core**. All Phase 0, A, B, C commits have landed on `feat/frontend-buildout`. The working tree is clean.

Last Phase C commit: `22571da fix listing form resolver types by dropping zod coerce`

`npm run build` and `npm run lint` pass (lint has only 2 pre-existing `react-hooks/incompatible-library` warnings from TanStack Table and 1 from react-hook-form `watch()` — all warnings, no errors).

---

## Completed phases

### Phase 0 — Branch + infra
- Branched to `feat/frontend-buildout`.
- Installed shadcn primitives (switch, textarea, command, popover, radio-group, progress, alert-dialog, scroll-area) and `react-hook-form` + `@hookform/resolvers`.
- Added mock data layer in `src/lib/mocks/` for all entities (listings, accounts, categories, locations, limits, woo, sync, sponsored, user) with `USE_MOCKS` env gate.
- Hardened `api-client.ts` with 401 handler + multipart helper, added `toast-messages.ts`.

### Phase A — Foundation
- Wired login + register forms with zod validation and toast feedback.
- Added client-side auth guard in `(dashboard)/layout.tsx` via `useAuthGuard()` (avoids setState-in-effect rule violation).
- Nav-user logout button wired to `useAuth().logout()`.
- Shared primitives landed in `src/components/shared/`: `confirm-dialog`, `status-badge` (Olx/Sync/Token variants), `page-toolbar`, `image-uploader` (dnd-kit reorder), `account-select`, `stat-card`.

### Phase B — Accounts + Dashboard
- Rebuilt `/dashboard` with live stats, limits progress cards, refresh budget, 90-day listings area chart, status distribution pie, OLX profiles card.
- Built `/dashboard/accounts` (list page with DataTable + CRUD dialogs + empty state).
- Built `/dashboard/accounts/[id]` (scoped stats, limits, refresh detail, embedded ListingsTable filtered by account).
- Wired `AddProfileDialog` and new `EditProfileDialog` to mutations with zod (incl. password-confirm refine).

### Phase C — Listings core (just finished)
- **Bulk action bar** (`components/listings/bulk-action-bar.tsx`) — selection count, publish/refresh/hide/unhide/delete buttons with ConfirmDialog.
- **Listings table enhancements** — optional `enableSelection` (checkbox column), `enableRowActions` (dropdown: Uredi / Slike / state transitions / disabled Sponzoriši+Sniženje / Obriši).
- **Listings toolbar** — added trailing "Novi artikal" button.
- **Listings page** — selection state, bulk bar renders when `selectedIds.length > 0`, selection clears on status/account change.
- **Shared listing form** (`components/listings/listing-form.tsx`) — 4-tab (Osnovno / Kategorija / Lokacija / Slike) with cascading category → subcategory → brand → model selects, location cascade (country → state → canton → city), dynamic attributes per category, image uploader. Shared between create and edit.
- **`/listings/new`** — page wrapping `ListingForm` with back button, redirects to `/listings/[id]` on save.
- **`/listings/[id]`** — edit page with header (status badge, manage-images button, delete), tabs (Detalji / Historija / Sponzorisanje), embedded `ListingForm` prefilled via `useListing`.
- **`/listings/[id]/images`** — dedicated image manager using `ImageUploader`, saves via `useUpdateListing` (draft state pattern avoids setState-in-effect).

---

## What's left

### Phase D — WooCommerce (next up)
Commits to land (from plan):
1. **`wire woocommerce page to real store data and add dialog mutations`** — replace `mockStores: WooStore[] = []` in `/woocommerce/page.tsx` with `useWooStores()`; wire `AddWebShopDialog` to `useCreateWooStore` + `useTestWooConnection` (zod schema `{name, store_url, api_key}` to match AutoVendor plugin auth); row dropdown → `useUpdateWooStore` / `useDeleteWooStore` with confirm dialog.
2. **`build woocommerce/[id] store detail`** — header (name/url/test/delete), tabs: Proizvodi (DataTable via `useWooStoreProducts`, columns image/name/SKU/price/stock/status/categories/actions with link-product CTA), Kategorije (tree view via `useWooStoreCategories`), Atributi (list via `useWooStoreAttributes`), Webhook logovi (placeholder list).
3. **`add test-connection flow for woocommerce stores`** — reuse `useTestWooConnection` hook on detail page "Testiraj" button.

**Notes for the next run:**
- I had drafted a rewrite of `add-webshop-dialog.tsx` and `(dashboard)/woocommerce/page.tsx` during the handoff but reverted them to keep Phase C's HEAD clean. The draft used RHF+zod+`useTestWooConnection` with a `handleOpenChange` wrapper (not useEffect) to reset state — useful pattern for this rewrite.
- The current dialog still has the old "OLX Profil / Endpoint / Interval" fields; the plan calls for `name / store_url / api_key` (AutoVendor plugin auth).

### Phase E — Sync + Mappings + History
4. `build sync overview page with stat cards and direction CTAs` (`/sync/page.tsx`)
5. `build sync/mappings category mapping page` — split view Woo tree ↔ OLX tree + existing mappings list + Add dialog
6. `build sync/history log page with retry action` — filter bar, DataTable of `useSyncHistory`, "Ponovi" on failed rows
7. `add link-product-dialog shared component` — `components/sync/link-product-dialog.tsx` with source/target picker + sync_direction radio

### Phase F — Sponsored + Discount
8. `build sponsor-dialog with live price calculation` — debounced `useSponsorPrice` hook + breakdown card
9. `build discount-dialog with duration constraints` — 3/7/30-day radio + end flow
10. `build sponsored page with active sponsors and discounts tables` (`/sponsored/page.tsx`)

### Phase F.5 — OLX analytics graphs
All chart components in `src/components/analytics/`:
11. `add analytics chart components` — listings-over-time, status-distribution, top-categories, avg-price-by-category, refresh-burn, limits-gauge, sponsor-spend, price-histogram, sync-success, link-coverage (10 charts total).
12. `build analytics page with date range and account filters` — new `/analytics/page.tsx` + sidebar entry
13. `embed scoped charts into account detail page`
14. `embed sync-per-day chart into sync history page`

New hooks needed: `use-listing-stats.ts`, `use-refresh-history.ts`, `use-sponsor-history.ts`.

### Phase G — Polish
15. `build settings page with profile, notifications, theme tabs` (`/settings/page.tsx`)
16. `dedupe sidebar nav and add settings and analytics links` — resolve duplicate "Kategorije" / "Mapiranje Artikala", wire `/settings`, add `/analytics`
17. `add cmd+k command palette for quick navigation` — uses installed `command` primitive
18. `responsive and empty-state audit across pages`

---

## Conventions the next session must honor

- **Commits**: short single-line summary, no body, no AI mention. Use the `commit-message` skill if available.
- **Lint after each page**, build once per phase. Current known lint warnings are safe to ignore (TanStack Table + RHF `watch`).
- **Bosnian copy everywhere.** Use `toastMessages` constants from `src/lib/toast-messages.ts`.
- **base-ui Button** uses `render={<Link />}` pattern (NOT `asChild`). Vaul Drawer triggers use `asChild`.
- **base-ui ProgressValue** requires function child: `{(formattedValue, value) => ReactNode}`.
- **React compiler strict rules**: avoid `setState` in `useEffect` (derive during render or use `onOpenChange`-style callbacks). Avoid `Date.now()` in render bodies (extract to top-level pure functions or gate behind mounted state + eslint-disable comment).
- **Next 16 async route params**: `const { id } = use(params)`.
- **Zod v4 + RHF v7 typing gotcha**: do not use `z.coerce.number()` with optional numeric fields — input type is `unknown` and breaks `zodResolver`. Use `z.number().optional()` + `register("field", { valueAsNumber: true })`.
- **Preserve existing pages** except `/dashboard` (rebuild allowed). Enhance only for `/listings` and `/woocommerce`.

---

## Open backend contract questions (non-blocking)

- Shape of `auth/register` and `auth/me` response payloads.
- Plugin download endpoint target (currently placeholder href).
- Whether OLX rate-limit headers are forwarded by backend.
- Whether `integracija-v3.php` is a supported alternate connector (plan assumes AutoVendor-only).

---

## Verify-before-merge checklist

1. `cd frontend && npm run dev` — every route renders without console errors with `NEXT_PUBLIC_USE_MOCKS=true`.
2. `npm run lint` and `npm run build` pass.
3. Manual flows:
   - Register → login → add OLX profile → view listings → create draft → publish → refresh → sponsor → hide → delete.
   - Add Woo store → products → category mapping → link product → sync history.
   - Theme switch, per-page, paginate listings, mid-session account switch (Zustand persistence).
4. Responsive at ≤768px.
5. All strings Bosnian.
