# Short Video Application Requirements Specification

## 1. Purpose

This document defines the functional and non-functional requirements for
a TikTok-like vertical short-video application.

## 2. Product Main Goals(important)

-   Fast vertical video browsing
-   Smooth streaming over unreliable networks
-   Simple video upload workflow
-   Social interaction
-   Secure and scalable cloud architecture

## 3. Target Platform

-   Android only (Kotlin + Jetpack Compose)
-   Linux API Server (Node.js + TypeScript + Express)
-   Managed PostgreSQL
-   Managed Redis
-   Docker deployment
-   Cloudflare Stream or AWS VOD
-   Firebase Cloud Messaging


## 4. Feed & Video Playback

### 4.1 Vertical Feed

-   `GET /v1/feed` returns a cursor-paginated list of vertically-swipeable
    short videos (`items`, `nextCursor`, `hasMore`).
-   Supports a `tab` query parameter: `foryou` (default, algorithmic/global
    ranking) or `following` (videos authored only by accounts the current
    user follows). The `following` tab requires authentication; anonymous
    or logged-out requests receive an empty page.
-   Each feed item includes: playback URLs (`streamUrl`, `playbackFormat`,
    `streamUrlExpiresAt`), `thumbnailUrl`, author identity
    (`authorId`, `authorName`, `authorAvatarUrl`), `description`,
    `hashtags`, `category`, `durationMs`, `musicLabel`, engagement counters
    (`likeCount`, `commentCount`, `shareCount`), and viewer-relative state
    (`isLiked`, `isFollowing`, `isSaved`).
-   Impressions are deduplicated per audience (authenticated user id, or
    anonymous device id) so a viewer does not see the same video twice
    within a scroll session; the impression history resets once the whole
    catalog (or followed-authors catalog, for the `following` tab) has been
    exhausted.
-   The first page of the `foryou` tab may be served from a short-lived
    (30 second) Redis cache keyed by audience to reduce database load under
    high concurrency. Caching is skipped transparently when Redis is not
    configured or unavailable.

### 4.2 Playback Telemetry

-   `POST /v1/playback/events/batch` accepts up to 50 batched playback
    events per call: `PLAY`, `PAUSE`, `SEEK`, `PROGRESS`, `COMPLETE`,
    `BUFFER`, `IMPRESSION`, and `TTFF` (time-to-first-frame, used for
    startup latency analytics).
-   Events reference a `videoId`, `positionMs`, and optional `occurredAt`
    timestamp, and are attributed to the authenticated user and/or device.

## 5. Social Interaction

-   **Likes** — `POST /v1/videos/:videoId/like` and
    `DELETE /v1/videos/:videoId/like` toggle a like and return the updated
    `liked` state and `likeCount`. Liking a video notifies its owner.
-   **Comments** — `GET /v1/videos/:videoId/comments` returns a
    cursor-paginated list of comments; `POST /v1/videos/:videoId/comments`
    creates a comment (`text`) and notifies the video owner.
-   **Follow** — `POST /v1/users/:userId/follow` and
    `DELETE /v1/users/:userId/follow` manage the follower graph and return
    the resulting `following` state and `followerCount`. Following a user
    notifies them. Self-follow is rejected.
-   **Save (bookmark)** — `POST /v1/videos/:videoId/save` and
    `DELETE /v1/videos/:videoId/save` let a user bookmark a video for later.
-   Engagement counters (`likeCount`, `commentCount`, `shareCount`) are
    denormalized on the `Video` record for fast feed rendering, and kept in
    sync transactionally as likes/comments are created or removed.

## 6. Video Upload & Publishing

-   Upload uses a direct-to-Cloudflare-Stream (or compatible provider)
    signed URL flow: `POST /v1/uploads` creates an upload session and
    returns an `uploadUrl` and short-lived `uploadToken`.
-   Clients report progress via `PATCH /v1/uploads/:uploadId/progress` and
    finalize the asset through the provider's webhook
    (`POST /v1/webhooks/*`), which transitions the video to `PROCESSING`
    and then `READY`.
-   `POST /v1/videos/:videoId/publish` attaches the final `description`,
    `hashtags`, and `category` and marks the video `READY` for the feed.
-   A single active upload per user is enforced; in-flight uploads can be
    cancelled with `DELETE /v1/uploads/:uploadId`.
-   Thumbnails default to the Cloudflare Stream-generated frame
    (`https://videodelivery.net/{assetId}/thumbnails/thumbnail.jpg`) unless
    an explicit `thumbnailUrl` is provided.

## 7. User Profile & Discovery

-   `GET /v1/users/me/profile` returns the authenticated user's own profile
    (follower/following counts, total videos, total likes received).
-   `GET /v1/users/:userId/profile` returns the same shape for any user,
    plus `isFollowing` relative to the requester when authenticated.
-   `GET /v1/users/:userId/videos` returns a cursor-paginated grid of a
    user's published videos (`id`, `thumbnailUrl`, `likeCount`,
    `durationMs`) for the profile page.
-   `GET /v1/discover?q=` powers search and the trending/explore surface:
    with no query it returns trending hashtags and videos; with a query it
    searches hashtags, usernames/display names, and video descriptions,
    returning `{ hashtags, users, videos }`.

## 8. Notifications (Inbox & Push)

-   `GET /v1/inbox` returns a cursor-paginated list of in-app notifications
    (`LIKE`, `COMMENT`, `FOLLOW`, `SYSTEM`, `ANNOUNCEMENT`) plus an
    `unreadCount`.
-   `POST /v1/inbox/read-all` and `POST /v1/inbox/:id/read` mark
    notifications as read.
-   `POST /v1/devices/fcm` registers/updates a Firebase Cloud Messaging
    token for the caller's device, enabling push delivery. Every
    in-app notification also attempts a push dispatch to the user's
    registered device tokens; the push transport is a pluggable stub in
    this codebase until `firebase-admin` credentials are configured, so
    notifications are always persisted even if push delivery is skipped.

## 9. Reporting & Moderation

-   `POST /v1/reports` lets an authenticated user flag a `VIDEO`, `USER`,
    or `COMMENT` with a `reason`. Reports start in the `OPEN` status and
    feed the admin moderation queue described in Section 10.

## 10. Admin & Analytics API

All endpoints below live under `/v1/admin` and require an authenticated
session whose account role is `ADMIN`.

-   `GET /v1/admin/users` / `PATCH /v1/admin/users/:userId` — paginated
    user directory and the ability to change a user's `status`
    (`ACTIVE`/`SUSPENDED`/`DELETED`) or `role` (`USER`/`ADMIN`).
-   `GET /v1/admin/videos` / `PATCH /v1/admin/videos/:videoId` — paginated
    video moderation queue (optionally filtered by `status`) and the
    ability to force a video's `status` (e.g. take down content).
-   `GET /v1/admin/reports` / `PATCH /v1/admin/reports/:id` — paginated
    user reports (optionally filtered by `status`) and the ability to
    resolve or dismiss them.
-   `GET /v1/admin/announcements` / `POST /v1/admin/announcements` — list
    and publish platform-wide announcements shown to end users.
-   `GET /v1/admin/analytics` — a snapshot of core metrics: `userCount`,
    `videoCount`, `readyVideoCount`, `openReportCount`, `likeCount`,
    `commentCount`.

## 11. Data Model Overview

Beyond the core `User`, `Video`, and upload/playback tables introduced in
earlier phases, the social and admin features add:

-   `VideoLike`, `VideoComment`, `VideoSave` — per-user engagement records,
    unique per `(videoId, userId)` for likes/saves.
-   `Follow` — directed follower graph, unique per
    `(followerId, followingId)`.
-   `Report` — moderation queue entries with a polymorphic
    `targetType`/`targetId` pair.
-   `Announcement` — admin-authored platform messages.
-   `Notification` — per-user inbox entries, optionally linked to a
    `videoId` and/or an `actorUserId` (the user who triggered the
    notification).
-   `UserDevice.fcmToken` — the device's current push token, updated via
    `POST /v1/devices/fcm`.

## 12. Non-Functional Requirements

-   **Consistency** — All endpoints follow the shared API envelope:
    successful responses are `{ data, request_id }`; failures are
    `{ error: { code, message, request_id } }`.
-   **Validation** — Every request body/query is validated with Zod
    schemas before reaching business logic; invalid input yields a `400
    VALIDATION_ERROR`.
-   **AuthZ** — Endpoints that mutate personal or social state require a
    valid Bearer access token (`authenticate` middleware); endpoints whose
    response varies by viewer (profile, feed, comments) accept optional
    authentication (`optionalAuthenticate`) so anonymous users still get a
    sensible response. Admin endpoints additionally require `role ===
    ADMIN` (`requireAdmin` middleware).
-   **Performance** — Feed queries are cursor-paginated (never offset-based)
    to stay performant as the video catalog grows; the `foryou` first page
    may be cached in Redis for up to 30 seconds per audience.
-   **Resilience** — Redis and the background job queue (BullMQ) are
    optional at runtime: if unavailable, the system degrades gracefully
    (no caching, synchronous inline processing) rather than failing
    requests.
-   **Observability** — All requests are structured-logged (pino) with a
    `request_id` correlation id that is echoed back to the client.
-   **Security** — Passwords are hashed with bcrypt; access/refresh tokens
    are short-lived JWTs; rate limiting is applied to authentication
    routes; Helmet security headers and CORS allow-listing are enabled by
    default.

## 13. Admin Console

-   User management
-   Video moderation
-   Report management
-   Announcement management
-   Analytics dashboard

A minimal reference implementation lives in `ShortVideo_admin/` — a
Vite + React + TypeScript single-page app that authenticates against
`POST /v1/auth/login` and drives the `/v1/admin/*` API described in
Section 10 through tabs for Users, Videos, Reports, Announcements, and
Analytics.
