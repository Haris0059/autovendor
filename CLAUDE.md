# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

AutoVendor — a web dashboard connecting OLX.ba stores with WooCommerce for multi-account management, bidirectional product sync, and OLX operations. Currently frontend-only (no backend yet); the backend API is expected at `localhost:8000`.

## Commands

All commands run from the `frontend/` directory:

```bash
# Start dev server (localhost:3000)
npm run dev

# Production build
npm run build

# Lint
npm run lint

# Start infrastructure (PostgreSQL + Redis)
docker compose up -d   # run from repo root
```

## Architecture

### Stack
- Next.js 16 / React 19 / TypeScript / Tailwind CSS 4 / shadcn/ui
- Zustand for client state, TanStack React Query for server state, TanStack React Table for data tables
- Recharts for dashboard charts, dnd-kit for drag-and-drop, Vaul for drawers, Sonner for toasts

### Routing (App Router)
- `(auth)/` — login, register (separate layout, no sidebar)
- `(dashboard)/` — all authenticated routes (shared layout with sidebar + header)
  - `dashboard/` — summary cards, charts, data table
  - `dashboard/accounts/` — OLX Profili (multi-account management)
  - `listings/` — Artikli page, profile-scoped listings table + toolbar
  - `woocommerce/` — Web shop connect dialog, import table, usage info dialog
  - `sync/`, `sponsored/`, `settings/` — placeholder/in-progress

### Key directories
- `src/components/ui/` — shadcn/ui primitives (do not edit manually; use `npx shadcn add`)
- `src/components/` — app-level components (sidebar, dialogs, data tables, page-specific components in subdirectories like `dashboard/`, `listings/`, `sync/`)
- `src/components/layout/` — layout components
- `src/components/shared/` — shared components used across pages (`data-table.tsx`, `page-header.tsx`, `empty-state.tsx`)
- `src/components/listings/` — `listings-table.tsx`, `listings-toolbar.tsx` (Artikli page)
- `src/providers/` — React context providers (theme, React Query, combined `providers.tsx`)
- `src/lib/api-client.ts` — singleton `api` client wrapping fetch with JWT auth (token from localStorage)
- `src/lib/utils.ts` — `cn()` utility for Tailwind class merging

### API client pattern
The `api` client in `src/lib/api-client.ts` talks to an external backend (not Next.js API routes). Auth uses Bearer tokens stored in localStorage. No Next.js API routes exist.

### Other directories
- `olx-api-docs/` — reverse-engineered OLX.ba API documentation (auth, listings, categories, locations, etc.)
- `wp-plguin/` — AutoVendor WooCommerce plugin (`autovendor.php`). Read-only REST API (`/wp-json/autovendor/v1`) for catalog, hashes, stock, categories, attributes, plus webhooks for product changes. Auth via `X-AutoVendor-API-Key` header. See `wp-plguin/README.md` for endpoints.
- `backend/` — backend planning only so far (`PLAN.md`); no implementation yet

## Conventions

- Product name is **AutoVendor** (not "OLX Dashboard")
- UI components come from shadcn/ui — add new ones with `npx shadcn add <component>` from `frontend/`
- Route groups use parenthesized folders: `(auth)` and `(dashboard)`
