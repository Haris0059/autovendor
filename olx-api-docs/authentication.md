# Authentication

## Login / Get Token

**Endpoint:** `POST /auth/login`

**Headers:**
- `Content-Type: application/json`

**Required Parameters:**

| Parameter     | Type   | Description                          |
|---------------|--------|--------------------------------------|
| `username`    | string | Username or email                    |
| `password`    | string | User's password                      |
| `device_name` | string | Device identifier (e.g. "integration") |

**Example Request:**
```bash
curl -X POST https://api.olx.ba/auth/login \
  -d username="test@olx.ba" \
  -d password="password" \
  -d device_name="integration"
```

**Response:**
```json
{
  "token": "163|1bA8cqxhtoohFDROFAWYPGhkvYApzLpm2ojzD6Tc",
  "user": {
    "id": 1,
    "type": "shop",
    "email": "email@olx.ba",
    "username": "OLX",
    "first_name": "...",
    "last_name": "..."
  }
}
```

## Using the Token

Include the returned token in all subsequent requests via the `Authorization` header:

```
Authorization: Bearer {token}
```

**Notes:**
- Unauthenticated requests return `404` or `403`
- All requests must use HTTPS

## Get Current User

**Endpoint:** `GET /me`

```bash
curl https://api.olx.ba/me -H "Authorization: Bearer {token}"
```
