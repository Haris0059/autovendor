# Locations

All endpoints require:
- `Content-Type: application/json`
- `Authorization: Bearer {token}`

---

## Get Countries

**`GET /countries`**

**Response:**
```json
[
  {
    "id": 1,
    "name": "Bosnia and Herzegovina",
    "code": "BA"
  }
]
```

---

## Get Country States

**`GET /country-states`**

**Response:**
```json
[
  { "id": 1, "name": "Federacija BiH" },
  { "id": 2, "name": "Republika srpska" },
  { "id": 3, "name": "Brcko Distrikt" }
]
```

---

## Get All Cities

**`GET /cities`**

Returns an array of city objects with id, name, code, and cantons.

---

## Get City Details

**`GET /cities/:id`**

**Response:**
```json
{
  "id": 1,
  "name": "Sarajevo",
  "zip_code": "71000",
  "latitude": 43.856,
  "longitude": 18.413,
  "parent_id": null,
  "population": 275000,
  "country_id": 1,
  "canton_id": 9,
  "state_id": 1
}
```

---

## Get Cities in a Canton

**`GET /cantons/:id/cities`**

Returns an array of cities within the specified canton:
```json
[
  {
    "id": 1,
    "name": "Sarajevo",
    "location": {
      "lat": 43.856,
      "lon": 18.413
    },
    "canton_id": 9
  }
]
```
