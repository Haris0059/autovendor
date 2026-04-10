# AutoVendor

A web dashboard that connects OLX.ba stores with WooCommerce, allowing users to manage multiple OLX profiles, sync products bidirectionally, and handle all OLX operations from a single interface.

## Tech Stack

- **Frontend:** Next.js 16, React 19, TypeScript, Tailwind CSS 4, shadcn/ui
- **Database:** PostgreSQL 16
- **Cache/Queue:** Redis 7
- **ORM:** Prisma
- **Auth:** JWT with AES-256 credential encryption

## Getting Started

### Prerequisites

- Node.js 18+
- Docker & Docker Compose

### Setup

```bash
# Clone the repo
git clone https://github.com/Haris0059/autovendor.git
cd autovendor

# Start database and Redis
docker compose up -d

# Setup frontend
cd frontend
cp ../.env.example .env.local
npm install
npm run dev
```

The app runs at `http://localhost:3000`.

### Environment Variables

Copy `.env.example` and fill in the values:

| Variable | Description |
|----------|-------------|
| `DATABASE_URL` | PostgreSQL connection string |
| `REDIS_URL` | Redis connection string |
| `JWT_SECRET_KEY` | Secret for JWT tokens |
| `ENCRYPTION_KEY` | 32-byte hex key for AES-256 |
| `OLX_API_BASE_URL` | OLX.ba API endpoint |
| `NEXT_PUBLIC_API_URL` | Backend API URL for frontend |

## Current Features

- Login and registration pages (shadcn UI)
- Dashboard with charts, data table, and summary cards
- Sidebar navigation with SPA client-side routing
- OLX Profili page (add/edit/remove accounts with modals)
- Artikli (listings) page with profile-scoped listings table and toolbar
- WooCommerce page with web shop connect dialog, import table, and usage info dialog
- **AutoVendor WooCommerce plugin** — WordPress plugin exposing a read-only REST API and webhooks for product data (see [`wp-plguin/README.md`](wp-plguin/README.md))
- Placeholder pages for remaining planned routes

## Planned Features

- **OLX Integration:** Multi-account management, token auto-refresh, listing CRUD, image management, bulk actions, sponsored listings
- **WooCommerce Integration:** Store connections, product fetching, category mapping
- **Product Sync:** Bidirectional sync (WooCommerce <-> OLX), scheduled background jobs, sync history and logs
- **Polish:** Responsive design, error handling, notifications, rate limiting

## Project Structure

```
autovendor/
├── frontend/              # Next.js application
│   └── src/
│       ├── app/
│       │   ├── (auth)/    # Login, register
│       │   └── (dashboard)/ # Dashboard, listings, woocommerce, sync, etc.
│       ├── components/    # UI components, sidebar, dialogs, page-specific
│       └── providers/     # Theme, query, tooltip providers
├── wp-plguin/             # AutoVendor WooCommerce plugin (PHP)
├── olx-api-docs/          # OLX.ba API documentation
├── docker-compose.yml     # PostgreSQL + Redis
└── .env.example           # Environment variables template
```

## License

Private project.
