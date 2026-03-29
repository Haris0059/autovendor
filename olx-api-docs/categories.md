# Categories

All endpoints require:
- `Content-Type: application/json`
- `Authorization: Bearer {token}`

---

## Get All Top-Level Categories

**`GET /categories`**

Returns all top-level categories.

---

## Get Child Categories

**`GET /categories/:id`**

Returns child categories for a given parent category ID.

---

## Get Single Category

**`GET /category/:id`**

Returns a single category object:
```json
{
  "id": 1,
  "name": "Automobili",
  "name_singular": "Automobil",
  "slug": "automobili",
  "parent_id": null,
  "order": 1,
  "top_category": true,
  "highlighted": true,
  "shipping_available": false,
  "sensitive_content": false,
  "show_price": true,
  "show_brand": true,
  "show_condition": true,
  "show_map": true,
  "listing_fee": 0,
  "base_listing_price": 0,
  "icon": "..."
}
```

---

## Get Category Attributes

**`GET /categories/:id/attributes`**

Returns attributes specific to a category (used when creating listings).

**Attribute Object:**
```json
{
  "id": 1,
  "type": "select",
  "name": "fuel_type",
  "input_type": "dropdown",
  "display_name": "Gorivo",
  "options": [...],
  "rank": 1,
  "order": 1,
  "required": true,
  "highlighted": true
}
```

---

## Get Category Brands

**`GET /categories/:id/brands`**

Returns brands available in a category.

**Brand Object:**
```json
{
  "id": 1,
  "name": "BMW",
  "slug": "bmw"
}
```

---

## Get Brand Models

**`GET /categories/:id/brands/:brand_id/models`**

Returns models for a specific brand within a category.

**Model Object:**
```json
{
  "id": 1,
  "name": "Series 3",
  "slug": "series-3"
}
```

---

## Suggest Categories

**`GET /categories/suggest?keyword=:keyword`**

Suggests categories based on a keyword.

---

## Find Categories by Name

**`GET /categories/find?name=:name`**

Searches categories by name.
