# ShortVideo — Local Test Guide & Checklist

Yes — you can test almost everything **locally**. Cloudflare Stream and real FCM are optional for a first pass; the Android app falls back to mock sample videos if the feed API is empty or unreachable.

---

## 1. Can I test locally?

| Surface | Local? | Notes |
|---------|--------|--------|
| Android app (feed UI, swipe, auth, upload UI) | Yes | Emulator or physical device |
| API server | Yes | `npm run dev` or Docker |
| PostgreSQL + Redis | Yes | Docker Compose |
| Admin console | Yes | Vite on `localhost:5173` |
| Feed with real HLS | Partial | Needs Cloudflare Stream credentials + published videos |
| Feed without cloud | Yes | Mock MP4 URLs when API empty/fails |
| Upload → Stream processing | Partial | Needs Cloudflare; otherwise use `dev-complete` in non-prod |
| Real push (FCM) | No (stub) | Token registers to API; push delivery is a log stub until Firebase is configured |

**Recommended first local path:** Docker (Postgres + Redis + API) → Android emulator → Admin in browser. Skip Cloudflare and FCM until feed/social UI is solid.

---

## 2. Services required

### Minimum (local MVP)

| Service | Port | How to start | Required for |
|---------|------|--------------|--------------|
| **PostgreSQL** | `5432` | `docker compose up` | Auth, feed DB, social, admin |
| **Redis** | `6379` | `docker compose up` | Rate limit, optional feed cache, upload queue |
| **API (Express)** | `3000` | Compose or `npm run dev` | Everything except pure mock feed |
| **Android app** | — | Android Studio / Gradle | Client QA |

### Optional

| Service | Required for | Without it |
|---------|--------------|------------|
| **Cloudflare Stream** | Real HLS upload/playback | Mock feed MP4s; upload may use dev URLs |
| **Admin console** (`ShortVideo_admin`) | Moderation UI | Use Swagger/curl against `/v1/admin` |
| **Firebase / FCM** | Real device push | Inbox still works in-app; push is stubbed |
| **Physical phone** | Real network / camera | Emulator is enough for most checks |

### Local URLs

| Client | `API_BASE_URL` / base URL |
|--------|---------------------------|
| Android **emulator** | `http://10.0.2.2:3000/` |
| Android **physical device** (same Wi‑Fi) | `http://YOUR_PC_LAN_IP:3000/` |
| Admin / browser / curl | `http://localhost:3000` |

Set Android base URL in `ShortVideo_android/local.properties`:

```properties
sdk.dir=C:\\Users\\YOU\\AppData\\Local\\Android\\Sdk
API_BASE_URL=http://10.0.2.2:3000/
```

---

## 3. How to start a local test session

### A. Backend stack (Docker)

```bash
cd ShortVideo_backend
cp .env.example .env
# Edit .env if needed (DATABASE_URL / REDIS_URL usually match Compose)
docker compose up --build
```

Smoke checks:

```bash
curl http://localhost:3000/health/live
curl http://localhost:3000/health/ready
curl http://localhost:3000/v1/feed?limit=5
```

Apply DB schema if needed:

```bash
cd ShortVideo_backend
npm install
npm run db:generate
npm run db:migrate
# or: npm run db:push
```

Automated API tests:

```bash
cd ShortVideo_backend
npm test
```

### B. Backend without full Docker (API only)

1. Start Postgres + Redis (Compose services only, or local installs).
2. `cp .env.example .env` and point `DATABASE_URL` / Redis URL.
3. `npm run db:migrate` then `npm run dev`.

### C. Android app

1. Open `ShortVideo_android` in Android Studio.
2. Confirm `local.properties` has `sdk.dir` and `API_BASE_URL`.
3. Run on emulator (API 26+).
4. Complete accessibility onboarding if prompted.
5. Home should show videos (API or mock fallback).

### D. Admin console (optional)

```bash
cd ShortVideo_admin
npm install
# optional: copy .env.example → .env with VITE_API_BASE_URL=http://localhost:3000
npm run dev
```

Open `http://localhost:5173`. Login needs a user with `role = ADMIN` in Postgres.

### E. Create a test admin (SQL example)

After migrations, promote a registered user:

```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'you@example.com';
```

Or register via `POST /v1/auth/register`, then update role as above.

---

## 4. What to test — checklist

Use this as a manual QA list. Check boxes as you go.

### 4.1 Environment & health

- [ ] `GET /health/live` returns OK
- [ ] `GET /health/ready` shows Postgres (and Redis if expected)
- [ ] Android app builds and launches
- [ ] App can reach API (`10.0.2.2:3000` on emulator) — or clearly falls back to mock feed

### 4.2 Auth

- [ ] Register new account
- [ ] Login with email/password
- [ ] Access token used on authenticated calls
- [ ] Logout clears session
- [ ] Password reset flow (request + complete if email/dev path works)
- [ ] Upload / Profile tabs redirect to login when logged out

### 4.3 Home feed — fast vertical browsing (main goal)

- [ ] Full-bleed vertical video (edge to edge)
- [ ] Swipe up/down snaps to next/previous clip
- [ ] Next video starts quickly (little/no long black frame on Wi‑Fi)
- [ ] Poster/thumbnail shows while buffering (when URL present)
- [ ] Tap pause / play
- [ ] Horizontal drag seeks within clip
- [ ] Mute toggle works and persists
- [ ] Progress bar stays subtle at bottom
- [ ] Pagination loads more near end of list
- [ ] Top tabs: **For You** / **Following**
- [ ] Search icon opens Discover
- [ ] Right rail visible: avatar, like, comment, save, share
- [ ] Double-tap shows heart animation and likes
- [ ] Bottom meta: `@author`, caption, hashtags, music marquee
- [ ] Bottom nav: Home / Discover / Upload / Inbox / Profile (dark on Home)

### 4.4 Social

- [ ] Like / unlike updates count and survives relaunch (logged in)
- [ ] Comment sheet opens; list loads; post comment works
- [ ] Follow via `+` on avatar; Following tab shows that creator’s videos
- [ ] Save / unsave bookmark
- [ ] Share increments or opens share affordance (local OK)
- [ ] Unauthenticated write actions prompt login (or fail gracefully)

### 4.5 Upload

- [ ] Pick from gallery
- [ ] Capture with camera (device/emulator camera)
- [ ] Preview plays
- [ ] Publish with description / hashtags
- [ ] Progress / completion UI
- [ ] After Cloudflare (or dev-complete), video appears in feed/profile

### 4.6 Profile

- [ ] Own profile: avatar, name, bio, counts
- [ ] 3-column video grid
- [ ] Settings: logout / delete account / password reset entry
- [ ] Other user profile + follow (if navigated)

### 4.7 Discover

- [ ] Trending hashtags shown
- [ ] Search query returns users / hashtags / videos (or mock)
- [ ] Video thumbs render

### 4.8 Inbox & push stub

- [ ] Inbox lists notifications after like/comment/follow on your content
- [ ] Mark one read / mark all read
- [ ] Unread count updates
- [ ] App registers device token (`POST /v1/devices/fcm`) without crash  
  *(real push not required locally)*

### 4.9 Admin console

- [ ] Login as ADMIN
- [ ] Analytics numbers load
- [ ] Users: list, change role/status
- [ ] Videos: filter/moderate status
- [ ] Reports: resolve/dismiss
- [ ] Announcements: create / list

### 4.10 Reports & abuse

- [ ] `POST /v1/reports` creates OPEN report (curl or future UI)
- [ ] Report appears in admin Reports tab

### 4.11 Network / resilience

- [ ] Airplane mode → player error overlay with retry/skip
- [ ] Kill API mid-session → Home still shows mock or cached behavior without crash
- [ ] Slow network: preload still helps next swipe (manual feel check)

### 4.12 Automated (backend)

- [ ] `npm test` in `ShortVideo_backend` passes (or skips cleanly without DB as documented)
- [ ] `npm run lint` (`tsc --noEmit`) passes

---

## 5. Suggested test order (local afternoon)

1. `docker compose up` → health curls  
2. Register + login (curl or app)  
3. Android Home: swipe, UI chrome, mute, seek  
4. Like / comment / follow (need two accounts for best follow/inbox test)  
5. Upload one short clip (Cloudflare or skip)  
6. Profile grid + Discover search  
7. Inbox after social actions  
8. Admin login + moderate a video / report  
9. Turn off Wi‑Fi briefly → error / retry  

---

## 6. Two-account social script (local)

1. **User A** — register, upload or use seeded feed video owned by A (or like A’s content).  
2. **User B** — register on second emulator/user or logout/login.  
3. B follows A → B **Following** tab shows A’s videos.  
4. B likes/comments A’s video → A **Inbox** shows notifications.  

---

## 7. Quick API checks (curl)

```bash
# Register
curl -s -X POST http://localhost:3000/v1/auth/register \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"a@test.com\",\"password\":\"Password1!\",\"username\":\"usera\",\"displayName\":\"User A\"}"

# Login — copy accessToken
curl -s -X POST http://localhost:3000/v1/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"a@test.com\",\"password\":\"Password1!\"}"

# Feed
curl -s "http://localhost:3000/v1/feed?limit=5&tab=foryou"

# Like (replace TOKEN and VIDEO_ID)
curl -s -X POST http://localhost:3000/v1/videos/VIDEO_ID/like \
  -H "Authorization: Bearer TOKEN"
```

Swagger UI (if enabled): open docs route from backend README / `/docs` as configured in the project.

---

## 8. What you cannot fully verify locally without extras

| Gap | Need |
|-----|------|
| Production HLS quality / signed URL expiry | Cloudflare Stream account + keys in `.env` |
| Real push notifications on lock screen | Firebase project + `google-services.json` + `firebase-admin` |
| True 3G stall metrics at scale | Network throttling + many real assets |
| Managed cloud Postgres/Redis failover | Cloud provider staging |

---

## 9. Pass / fail for “MVP TikTok-browsable”

**Pass** if all of these are true on local emulator + local API:

1. Vertical swipe feed feels continuous (no multi-second black gaps on Wi‑Fi).  
2. TikTok-style chrome present (tabs, right rail, bottom meta, bottom nav).  
3. Auth + at least one of: like, comment, or follow persists via API.  
4. App does not crash when API is down (mock or empty state).  

Everything else (Cloudflare upload, admin, FCM) can follow after this bar is green.
