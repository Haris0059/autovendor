# Listing Limits

All endpoints require:
- `Content-Type: application/json`
- `Authorization: Bearer {token}`

---

## Get Refresh Limits

**`GET /listing/refresh/limits`**

**Response:**
```json
{
  "free_limit": 10,
  "free_count": 3,
  "paid_count": 0,
  "listing_count": 15
}
```

---

## Get Listing Limits

**`GET /listing-limits`**

Returns category-based listing limits (e.g. how many listings you can post per category).

**Response includes limits for:**
- Cars
- Real estate
- Other categories

Each with listing counts and maximum allowed.
