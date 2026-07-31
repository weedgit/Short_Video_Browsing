# Short Video Android App

Kotlin + Jetpack Compose multi-module app for the short video service.

> **Repository:** This project is maintained as a standalone Git repository, separate from the backend API.

## Related repository

- Backend API: separate GitHub repository (Express + TypeScript)

Product requirements documents may live in a shared docs repo or wiki outside this repository.

## Modules

```text
├── app/                 # Entry point, navigation shell, bottom bar
├── core/                # Routes and shared primitives
├── domain/              # Use cases (Phase 2+)
├── data/                # Repositories, API, Room (Phase 2+)
├── common/
│   ├── theme/           # Material theme
│   └── composable/      # Shared Compose UI
└── feature/
    ├── home/
    ├── discover/
    ├── upload/
    ├── inbox/
    ├── profile/
    ├── auth/
    └── settings/
```

## Bottom navigation (Requirements UI-003)

`Home → Discover → Upload → Inbox → Profile`

Upload tab is visually emphasized (UI-004).

## Build

```bash
./gradlew :app:assembleDebug
```

Requires Android SDK and JDK 17.

## Backend API URL (physical device)

The app reads `API_BASE_URL` from `local.properties` (see `local.properties.example`).

| Target | `API_BASE_URL` |
|--------|----------------|
| Android emulator | `http://10.0.2.2:3000/` |
| Physical phone (same Wi‑Fi as dev PC) | `http://<PC_LAN_IP>:3000/` |

Find your PC IP on Linux:

```bash
ip -4 route get 1.1.1.1 | awk '{for(i=1;i<=NF;i++) if($i=="src") print $(i+1)}'
```

On the phone, open `http://<PC_LAN_IP>:3000/health/live` in a browser first. If that works, rebuild and install the app.

Requirements: phone and PC on the same Wi‑Fi, backend running on port `3000`, no client isolation on the router.

## CI

GitHub Actions runs on push/PR to `master`:

- `./gradlew :app:assembleDebug`

## Workflow

See [CONTRIBUTING.md](CONTRIBUTING.md) for branch, commit, and PR rules.

## GitHub setup

```bash
# After creating an empty repo on GitHub (e.g. shortvideo-android)
git remote add origin git@github.com:YOUR_USER/shortvideo-android.git
git push -u origin master

# Then enable branch protection on master (see CONTRIBUTING.md)
# All further changes: feat/* or fix/* branches → Pull Request → review → merge
```
