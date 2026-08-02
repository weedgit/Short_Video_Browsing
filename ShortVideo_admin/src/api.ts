import type {
  AdminAnalytics,
  AdminAnnouncement,
  AdminReport,
  AdminUser,
  AdminUserProfile,
  AdminVideo,
  AnnouncementListFilters,
  AuthSession,
  Page,
  ReportListFilters,
  UserListFilters,
  VideoListFilters,
} from "./types";

const DEFAULT_API_BASE_URL = "http://localhost:3000";
const ACCESS_TOKEN_STORAGE_KEY = "shortvideo_admin_access_token";
const USER_STORAGE_KEY = "shortvideo_admin_user";

export function getApiBaseUrl(): string {
  const configured = import.meta.env.VITE_API_BASE_URL?.trim();
  return configured && configured.length > 0 ? configured.replace(/\/$/, "") : DEFAULT_API_BASE_URL;
}

/** Resolve uploaded media (e.g. /avatars/...) against the API host used by admin. */
export function resolveMediaUrl(url: string | null | undefined): string | null {
  if (!url?.trim()) return null;
  const raw = url.trim();
  const base = getApiBaseUrl();

  if (raw.startsWith("/")) {
    return `${base}${raw}`;
  }

  try {
    const parsed = new URL(raw);
    if (parsed.pathname.startsWith("/avatars/")) {
      return `${base}${parsed.pathname}${parsed.search}`;
    }
  } catch {
    // Keep original string for non-URL values.
  }

  return raw;
}

export function getAccessToken(): string | null {
  return localStorage.getItem(ACCESS_TOKEN_STORAGE_KEY);
}

export function setSession(session: AuthSession | null): void {
  if (session) {
    localStorage.setItem(ACCESS_TOKEN_STORAGE_KEY, session.tokens.accessToken);
    localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(session.user));
  } else {
    localStorage.removeItem(ACCESS_TOKEN_STORAGE_KEY);
    localStorage.removeItem(USER_STORAGE_KEY);
  }
}

export function getStoredUser(): AuthSession["user"] | null {
  const raw = localStorage.getItem(USER_STORAGE_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as AuthSession["user"];
  } catch {
    return null;
  }
}

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly code: string,
    message: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers);
  headers.set("Content-Type", "application/json");

  const token = getAccessToken();
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const response = await fetch(`${getApiBaseUrl()}${path}`, { ...options, headers });
  const payload = await response.json().catch(() => null);

  if (!response.ok) {
    const code = payload?.error?.code ?? "UNKNOWN_ERROR";
    const message = payload?.error?.message ?? `Request failed with status ${response.status}`;
    throw new ApiError(response.status, code, message);
  }

  return (payload?.data ?? payload) as T;
}

function withQuery(path: string, params?: Record<string, string | number | undefined>): string {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params ?? {})) {
    if (value === undefined || value === "") continue;
    search.set(key, String(value));
  }
  const qs = search.toString();
  return qs ? `${path}?${qs}` : path;
}

// ---------- Auth ----------

export async function login(email: string, password: string): Promise<AuthSession> {
  return request<AuthSession>("/v1/auth/login", {
    method: "POST",
    body: JSON.stringify({ email, password }),
  });
}

// ---------- Users ----------

export function fetchUsers(page = 1, filters?: UserListFilters): Promise<Page<AdminUser>> {
  return request<Page<AdminUser>>(
    withQuery("/v1/admin/users", {
      page,
      q: filters?.q,
      role: filters?.role,
      status: filters?.status,
    }),
  );
}

export function updateUser(
  userId: string,
  data: Partial<{ status: AdminUser["status"]; role: AdminUser["role"] }>,
): Promise<AdminUser> {
  return request<AdminUser>(`/v1/admin/users/${userId}`, {
    method: "PATCH",
    body: JSON.stringify(data),
  });
}

// ---------- Videos ----------

export function fetchVideos(
  page = 1,
  filters?: VideoListFilters,
  limit?: number,
): Promise<Page<AdminVideo>> {
  return request<Page<AdminVideo>>(
    withQuery("/v1/admin/videos", {
      page,
      limit,
      status: filters?.status,
      q: filters?.q,
      hashtag: filters?.hashtag,
      category: filters?.category,
    }),
  );
}

export function fetchVideo(videoId: string): Promise<AdminVideo> {
  return request<AdminVideo>(`/v1/admin/videos/${videoId}`);
}

export function fetchUserProfile(userId: string): Promise<AdminUserProfile> {
  return request<AdminUserProfile>(`/v1/admin/users/${userId}`);
}

export function updateVideoStatus(videoId: string, status: AdminVideo["status"]): Promise<AdminVideo> {
  return request<AdminVideo>(`/v1/admin/videos/${videoId}`, {
    method: "PATCH",
    body: JSON.stringify({ status }),
  });
}

// ---------- Reports ----------

export function fetchReports(page = 1, filters?: ReportListFilters): Promise<Page<AdminReport>> {
  return request<Page<AdminReport>>(
    withQuery("/v1/admin/reports", {
      page,
      status: filters?.status,
      q: filters?.q,
    }),
  );
}

export function updateReportStatus(id: string, status: AdminReport["status"]): Promise<AdminReport> {
  return request<AdminReport>(`/v1/admin/reports/${id}`, {
    method: "PATCH",
    body: JSON.stringify({ status }),
  });
}

// ---------- Announcements ----------

export function fetchAnnouncements(
  page = 1,
  filters?: AnnouncementListFilters,
): Promise<Page<AdminAnnouncement>> {
  return request<Page<AdminAnnouncement>>(
    withQuery("/v1/admin/announcements", {
      page,
      q: filters?.q,
      active: filters?.active,
    }),
  );
}

export function createAnnouncement(data: {
  title: string;
  body: string;
  isActive: boolean;
}): Promise<AdminAnnouncement> {
  return request<AdminAnnouncement>("/v1/admin/announcements", {
    method: "POST",
    body: JSON.stringify(data),
  });
}

export function updateAnnouncement(
  id: string,
  data: Partial<{ title: string; body: string; isActive: boolean }>,
): Promise<AdminAnnouncement> {
  return request<AdminAnnouncement>(`/v1/admin/announcements/${id}`, {
    method: "PATCH",
    body: JSON.stringify(data),
  });
}

export function deleteAnnouncement(id: string): Promise<{ success: boolean }> {
  return request<{ success: boolean }>(`/v1/admin/announcements/${id}`, {
    method: "DELETE",
  });
}

// ---------- Analytics ----------

export function fetchAnalytics(range: 7 | 30 = 7): Promise<AdminAnalytics> {
  return request<AdminAnalytics>(`/v1/admin/analytics?range=${range}`);
}
