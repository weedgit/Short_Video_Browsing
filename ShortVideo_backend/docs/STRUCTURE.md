# Backend Structure Decision

## Proposed layout (layer-first)

```text
backend/
├── app.ts
├── server.ts
├── config/
├── routes/
├── controllers/
├── service/
├── middleware/
├── models/
├── utils/
├── db/
├── jobs/
└── integrations/
```

This matches the classic Express pattern shown in the project diagram:
`routes → controllers → service → models/db`.

## Comparison with Requirements (`06_backend_api_requirements.md`)

| Requirements (`modules/*`) | This project | Mapping |
|---|---|---|
| `modules/auth/` | `routes/auth.routes.ts` + `controllers/auth.controller.ts` + `service/auth.service.ts` | Same responsibility, different folder grouping |
| `modules/feed/` | `routes/feed.routes.ts` + ... | Domain prefix naming |
| `middleware/` | `middleware/` | Identical |
| `db/` | `db/` (Prisma) | Identical |
| `jobs/` | `jobs/` (BullMQ) | Identical |
| `integrations/` | `integrations/` | Identical |
| `common/` | `utils/` + `models/` | Shared helpers and DTO types |

## Why layer-first is acceptable

Requirements (EXPRESS-002) require **router / controller / service / repository separation**, not a specific folder name. Layer-first satisfies:

- EXPRESS-001: modular Express app
- EXPRESS-002: layer separation
- EXPRESS-003: no business logic in route handlers
- EXPRESS-004~015: validation, logging, ORM, workers, tests

## Scaling convention

As features grow, use **domain-prefixed files** inside each layer:

```text
controllers/
  auth.controller.ts
  feed.controller.ts
  upload.controller.ts
routes/
  auth.routes.ts
  feed.routes.ts
service/
  auth.service.ts
  feed.service.ts
db/
  repositories/
    user.repository.ts
    video.repository.ts
```

When a domain exceeds ~5 files per layer, introduce subfolders:

```text
service/upload/
  upload.service.ts
  upload-validation.service.ts
```

## Phase 0 status

Implemented:

- `app.ts`, `server.ts`
- Health routes/controllers/service
- Env validation (Zod), Helmet, CORS, Rate Limit, Pino
- Prisma scaffold, Docker, Vitest tests

## Phase 2 status (auth)

Implemented on `feat/phase-2-auth-api`:

- Prisma models: `users`, `refresh_tokens`, `password_reset_tokens`, `user_devices`
- JWT access + refresh token rotation
- `POST /v1/auth/register|login|refresh|logout`
- `POST /v1/auth/password/reset/request|confirm`
- `DELETE /v1/account`
- Auth rate limiting (Redis-backed when available, in-memory fallback)
- OpenAPI spec at `/openapi.json` and Swagger UI at `/docs`
- Auth rate limiting, Zod validation, Bearer middleware

Not yet implemented (later phases):

- Feed, upload, discover, inbox, admin modules
- BullMQ workers
- Cloud video / FCM integrations
- Redis-backed refresh token store (currently PostgreSQL)
