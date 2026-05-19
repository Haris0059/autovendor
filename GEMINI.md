# AutoVendor - Project Context & Guidelines

AutoVendor is a centralized dashboard for managing multiple **OLX.ba** accounts and synchronizing them with **WooCommerce** stores. It enables bidirectional product sync, bulk actions, and multi-account management from a single interface.

## Project Structure

- **`frontend/`**: Next.js 16 application (App Router) using React 19, TypeScript, Tailwind CSS 4, and shadcn/ui.
- **`backend/`**: FastAPI (Python) backend handling authentication, encrypted credential storage, and the sync engine (currently in the planning/implementation phase).
- **`wp-plguin/`**: Custom WordPress plugin providing a read-only REST API and webhooks for WooCommerce product data.
- **`olx-api-docs/`**: Reverse-engineered documentation for the OLX.ba API.
- **`docker-compose.yml`**: Infrastructure setup for PostgreSQL 16 and Redis 7.

## Tech Stack & Architecture

### Frontend (`frontend/`)
- **Framework**: Next.js 16 (App Router), React 19.
- **Styling**: Tailwind CSS 4, shadcn/ui (UI primitives).
- **State Management**: Zustand (client-side), TanStack React Query (server-state).
- **Data Tables**: TanStack Table (React Table).
- **Charts**: Recharts.
- **Form Handling**: React Hook Form + Zod.
- **API Client**: Custom wrapper in `src/lib/api-client.ts` pointing to `localhost:8000`.

### Backend (`backend/`) - *Planned/In-Progress*
- **Framework**: FastAPI (Python).
- **Database**: PostgreSQL with SQLAlchemy (async) and Alembic migrations.
- **Caching/Queue**: Redis for background jobs and category caching.
- **Auth**: JWT-based authentication.
- **Security**: AES-256 encryption for stored OLX and WooCommerce credentials.

### WordPress Plugin (`wp-plguin/`)
- Exposes `/wp-json/autovendor/v1` endpoints for catalog, hashes, and stock.
- Sends webhooks to the backend on product changes.

## Commands

### Development
Run these from the `frontend/` directory:
```bash
npm run dev      # Start Next.js development server (localhost:3000)
npm run lint     # Run ESLint
npm run build    # Production build
```

### Infrastructure
Run from the project root:
```bash
docker compose up -d    # Start PostgreSQL and Redis
```

## Development Conventions

- **Product Name**: Always refer to the project as **AutoVendor**.
- **UI Components**: Use `npx shadcn add <component>` to add new shadcn/ui primitives. Do not modify `src/components/ui/` manually unless necessary.
- **API Pattern**: The frontend communicates with the backend via the `api` client in `src/lib/api-client.ts`. Auth tokens are stored in `localStorage`.
- **Mocks**: The frontend includes a mock data layer in `src/lib/mocks/` gated by a `USE_MOCKS` environment variable for development without a running backend.
- **Tailwind**: Adhere to Tailwind CSS 4 conventions. Prefer utility classes over custom CSS where possible.
- **Safety**: Never log or commit sensitive credentials. The backend handles encryption for all third-party API keys and passwords.

## Key Files
- `CLAUDE.md`: Detailed guidance for AI agents (commands, architecture, conventions).
- `feature.md`: Project roadmap and feature status.
- `backend/PLAN.md`: Comprehensive backend architecture and sync logic documentation.
- `frontend/src/lib/api-client.ts`: Centralized API communication logic.
