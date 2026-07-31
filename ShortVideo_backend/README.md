# Short Video Backend

Express + TypeScript API server for the short video application.

> **Repository:** This project is maintained as a standalone Git repository, separate from the Android app.

## Related repository

- Android app: separate GitHub repository (Kotlin + Jetpack Compose)

## Directory structure

Layer-first Express layout (routes → controllers → service → models/db).

```text
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
├── integrations/
└── tests/
```

See `docs/STRUCTURE.md` for design rationale.

## Scripts

```bash
npm install
cp .env.example .env
npm run dev          # development with hot reload
npm run build        # compile TypeScript
npm run test         # run tests
npm run db:generate  # generate Prisma client
```

## Docker (local full stack)

```bash
cp .env.example .env
docker compose up --build
curl http://localhost:3000/health/live
```

## Health endpoints

- `GET /health/live` — process is running
- `GET /health/ready` — dependencies reachable
- `GET /v1` — API version info

## CI

GitHub Actions runs on push/PR to `master`:

- TypeScript lint
- Vitest tests
- Prisma client generation

## Workflow

See [CONTRIBUTING.md](CONTRIBUTING.md) for branch, commit, and PR rules.

## GitHub setup

```bash
# After creating an empty repo on GitHub (e.g. shortvideo-backend)
git remote add origin git@github.com:YOUR_USER/shortvideo-backend.git
git push -u origin master

# Then enable branch protection on master (see CONTRIBUTING.md)
# All further changes: feat/* or fix/* branches → Pull Request → review → merge
```
