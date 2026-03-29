# Users

All endpoints require:
- `Content-Type: application/json`
- `Authorization: Bearer {token}`

All endpoints support pagination via `page` (integer) parameter.

---

## Active Listings

**`GET /users/:username/listings`**

| Parameter | Type | Description  |
|-----------|------|--------------|
| `page`    | int  | Page number  |

**Example:**
```bash
curl -X GET https://api.olx.ba/users/alpus/listings \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}"
```

**Response:** Paginated array of listing objects with metadata:
```json
{
  "data": [ ... ],
  "total": 50,
  "last_page": 5,
  "current_page": 1,
  "per_page": 10
}
```

---

## Finished Listings

**`GET /users/:id/listings/finished`**

| Parameter | Type | Description  |
|-----------|------|--------------|
| `page`    | int  | Page number  |

---

## Inactive Listings

**`GET /users/:id/listings/inactive`**

| Parameter | Type | Description  |
|-----------|------|--------------|
| `page`    | int  | Page number  |

---

## Expired Listings

**`GET /users/:id/listings/expired`**

| Parameter | Type | Description  |
|-----------|------|--------------|
| `page`    | int  | Page number  |

---

## Hidden Listings

**`GET /users/:id/listings/hidden`**

| Parameter | Type | Description  |
|-----------|------|--------------|
| `page`    | int  | Page number  |
