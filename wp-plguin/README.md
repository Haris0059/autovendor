# AutoVendor WooCommerce Plugin

WordPress plugin that exposes a read-only REST API for WooCommerce product data. Installed on the store side so the AutoVendor dashboard can pull products, categories, and attributes for syncing to OLX.ba.

## Requirements

- WordPress 6.0+
- WooCommerce 8.0+

## Installation

1. Upload `autovendor.php` to `/wp-content/plugins/autovendor/`
2. Activate via **Plugins** in WordPress admin
3. Go to **WooCommerce → AutoVendor** to view your API key and configure webhook URL

## API Endpoints

Base: `https://yourstore.com/wp-json/autovendor/v1`

All requests require the `X-AutoVendor-API-Key` header.

| Endpoint | Description |
|---|---|
| `GET /catalog` | Full product data + categories + tags + attributes. Supports `page`, `per_page`, `updated_after`, `ids` params |
| `GET /catalog-hashes` | Product IDs + SHA-256 hashes for change detection |
| `GET /catalog-stock` | Product IDs + stock status/quantity |
| `GET /product/{id}` | Single product full data |
| `GET /categories` | Category hierarchy (nested tree) |
| `GET /attributes` | All attribute taxonomies + terms |

### Filtering

- `page` / `per_page` — pagination (default 50 for catalog, 200 for hashes/stock)
- `updated_after` — ISO8601 timestamp, returns only products modified after this date
- `ids` — comma-separated product IDs (catalog endpoint only), for bulk fetch after hash comparison

## Webhooks

Configure a webhook URL in **WooCommerce → AutoVendor** to receive real-time notifications on product changes:

- `product.created` — new product added
- `product.updated` — existing product modified
- `product.deleted` — product trashed or deleted

Webhook payload:
```json
{
  "event": "product.updated",
  "product_id": 123,
  "site_url": "https://yourstore.com",
  "timestamp": "2026-04-01T12:00:00+00:00"
}
```

## Settings

The admin page under **WooCommerce → AutoVendor** shows:

- **API Key** — auto-generated on activation, with copy and regenerate options
- **API Base URL** — the REST endpoint base for this store
- **Webhook URL** — where to send product change notifications
