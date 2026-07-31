# ShortVideo Admin Console

A minimal admin dashboard for the ShortVideo platform, built with
Vite + React + TypeScript. It authenticates against the backend's
`POST /v1/auth/login` endpoint and drives the `/v1/admin/*` API.

## Features

- **Login** with an admin account's email/password (a valid access token is
  stored in `localStorage` and attached as a `Bearer` token to every
  request).
- **Users** — list accounts, change `role` (`USER`/`ADMIN`) and `status`
  (`ACTIVE`/`SUSPENDED`/`DELETED`).
- **Videos** — moderation queue with status filtering and the ability to
  force a video's status (e.g. take down content).
- **Reports** — review user-submitted reports and mark them
  `RESOLVED`/`DISMISSED`.
- **Announcements** — publish platform-wide announcements.
- **Analytics** — a snapshot of core platform metrics.

## Getting started

```bash
npm install
npm run dev
```

The dev server runs on `http://localhost:5173` by default.

Configure the backend API location via an environment variable (copy
`.env.example` to `.env`):

```bash
VITE_API_BASE_URL=http://localhost:3000
```

If not set, the app defaults to `http://localhost:3000`, which matches the
`ShortVideo_backend` dev server.

## Requirements

The signed-in account must have `role = ADMIN` on the backend (see
`ShortVideo_backend/db/prisma/schema.prisma`, `UserRole` enum). You can
promote an existing account to admin directly in the database, or via the
Users tab once at least one admin account exists.

## Scripts

- `npm run dev` — start the Vite dev server.
- `npm run build` — type-check and build a production bundle to `dist/`.
- `npm run preview` — preview the production build locally.
- `npm run lint` — run TypeScript in `--noEmit` mode.
