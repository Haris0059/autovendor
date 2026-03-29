# Listings

All endpoints require:
- `Content-Type: application/json`
- `Authorization: Bearer {token}`

---

## Get Single Listing

**`GET /listings/:id`**

Returns a listing object with id, title, price, location, status, etc.

---

## Create Listing

**`POST /listings`**

Creates a new listing with **DRAFT** status. Must publish separately to activate.

**Required Parameters:**

| Parameter | Type   | Description    |
|-----------|--------|----------------|
| `title`   | string | Listing title  |

**Optional Parameters:**

| Parameter           | Type   | Description                              |
|---------------------|--------|------------------------------------------|
| `short_description` | string | Short description                        |
| `description`       | string | Full description                         |
| `country_id`        | int    | Country ID                               |
| `city_id`           | int    | City ID                                  |
| `price`             | number | Price                                    |
| `available`         | bool   | Availability                             |
| `listing_type`      | string | `sell`, `buy`, or `rent`                 |
| `state`             | string | `new` or `used`                          |
| `brand_id`          | int    | Brand ID                                 |
| `model_id`          | int    | Model ID                                 |
| `sku_number`        | string | SKU number                               |
| `attributes`        | array  | Category-specific attributes             |

**Example:**
```bash
curl -X POST https://api.olx.ba/listings \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "iPhone 15 Pro",
    "description": "Kao nov",
    "price": 1500,
    "city_id": 1,
    "listing_type": "sell",
    "state": "used"
  }'
```

**Response:** Listing object with assigned `id`.

---

## Update Listing

**`PUT /listings/:id`**

Updatable fields: title, description, price, and other listing attributes.

Returns the updated listing object.

---

## Delete Listing

**`DELETE /listings/:id`**

**Response:**
```json
{
  "message": "Uspjesno ste izbrisali oglas"
}
```

---

## Publish Listing

**`POST /listings/:id/publish`**

Activates a DRAFT listing, making it visible in search.

**Response:**
```json
{
  "message": "Oglas je uspjesno objavljen",
  "status": "active"
}
```

---

## Refresh / Boost Listing

**`PUT /listings/:id/refresh`**

Refreshes the listing date, boosting it in search rank.

---

## Finish Listing

**`POST /listings/:id/finish`**

Marks a listing as finished/completed.

---

## Hide Listing

**`POST /listings/:id/hide`**

Hidden listings won't show up in searches.

---

## Unhide Listing

**`POST /listings/:id/unhide`**

Reverses the hide action, making the listing visible again.

---

## Upload Images

**`POST /listings/:id/image-upload`**

| Parameter   | Type   | Description              |
|-------------|--------|--------------------------|
| `images`    | array  | Array of image files     |
| `image_url` | string | (optional) Image URL     |

**Example:**
```bash
curl -X POST https://api.olx.ba/listings/123/image-upload \
  -H "Authorization: Bearer {token}" \
  -F "images[]=@photo.jpg"
```

**Response:**
```json
[
  {
    "id": 1,
    "name": "photo.jpg",
    "sizes": {
      "sm": "...",
      "lg": "..."
    },
    "main": true
  }
]
```

---

## Delete Image

**`POST /listings/:id/image-delete`**

| Parameter | Type | Description |
|-----------|------|-------------|
| `imageId` | int  | Image ID    |

**Response:**
```json
{"success": true}
```

---

## Set Main Image

**`POST /listings/:id/image-main`**

| Parameter | Type | Description |
|-----------|------|-------------|
| `imageId` | int  | Image ID    |

**Response:**
```json
{"success": true}
```
