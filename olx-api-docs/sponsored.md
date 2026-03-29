# Sponsored & Paid Features

All endpoints require:
- `Content-Type: application/json`
- `Authorization: Bearer {token}`

**Note:** These are premium options. OLX credits will be charged.

---

## Sponsor a Listing

**`POST /listings/:id/sponsore`**

| Parameter       | Type    | Description                                  |
|-----------------|---------|----------------------------------------------|
| `type`          | int     | `0` = no sponsoring, `1` = normal, `2` = premium |
| `days`          | int     | 1, 2, 3, 5, 7, 14, 21, or 30                |
| `refresh_every` | int     | Auto-refresh interval: 0, 3, 6, 8, or 24 hours |
| `locations`     | array   | e.g. `["homepage"]`                          |

**Example:**
```bash
curl -X POST https://api.olx.ba/listings/123/sponsore \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "type": 1,
    "days": 5,
    "refresh_every": 3,
    "locations": ["homepage"]
  }'
```

---

## Get Sponsoring Price

**`GET /listings/:id/sponsore/price`**

Same parameters as sponsor endpoint (pass as query params).

**Response:**
```json
{
  "search": 50,
  "refresh": 100,
  "locations": 40,
  "extras": 0,
  "total": 190
}
```

---

## Set Discount Price

**`POST /listings/:id/discount`**

| Parameter | Type   | Description                |
|-----------|--------|----------------------------|
| `price`   | double | New discounted price       |
| `days`    | int    | Duration: 3, 7, or 30 only |

**Example:**
```bash
curl -X POST https://api.olx.ba/listings/123/discount \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "price": 100,
    "days": 7
  }'
```

---

## End Discount

**`POST /listings/:id/discount/finish`**

No additional parameters required. Ends the active discount on a listing.
