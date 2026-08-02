import type {
  AdminAnalytics,
  AdminAnnouncement,
  AdminReport,
  AdminUser,
  AdminVideo,
  AuthSession,
  Page,
} from "./types";

const DEFAULT_API_BASE_URL = "http://localhost:3000";
const ACCESS_TOKEN_STORAGE_KEY = "shortvideo_admin_access_token";
const USER_STORAGE_KEY = "shortvideo_admin_user";

export function getApiBaseUrl(): string {
  const configured = import.meta.env.VITE_API_BASE_URL?.trim();
  return configured && configured.length > 0 ? configured.replace(/\/$/, "") : DEFAULT_API_BASE_URL;
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

function withCursor(path: string, cursor?: string, extra?: Record<string, string | undefined>): string {
  const params = new URLSearchParams();
  if (cursor) params.set("cursor", cursor);
  for (const [key, value] of Object.entries(extra ?? {})) {
    if (value) params.set(key, value);
  }
  const qs = params.toString();
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

export function fetchUsers(cursor?: string): Promise<Page<AdminUser>> {
  return request<Page<AdminUser>>(withCursor("/v1/admin/users", cursor));
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

export function fetchVideos(cursor?: string, status?: AdminVideo["status"]): Promise<Page<AdminVideo>> {
  return request<Page<AdminVideo>>(withCursor("/v1/admin/videos", cursor, { status }));
}

export function updateVideoStatus(videoId: string, status: AdminVideo["status"]): Promise<AdminVideo> {
  return request<AdminVideo>(`/v1/admin/videos/${videoId}`, {
    method: "PATCH",
    body: JSON.stringify({ status }),
  });
}

// ---------- Reports ----------

export function fetchReports(cursor?: string, status?: AdminReport["status"]): Promise<Page<AdminReport>> {
  return request<Page<AdminReport>>(withCursor("/v1/admin/reports", cursor, { status }));
}

export function updateReportStatus(id: string, status: AdminReport["status"]): Promise<AdminReport> {
  return request<AdminReport>(`/v1/admin/reports/${id}`, {
    method: "PATCH",
    body: JSON.stringify({ status }),
  });
}

// ---------- Announcements ----------

export function fetchAnnouncements(): Promise<{ items: AdminAnnouncement[] }> {
  return request<{ items: AdminAnnouncement[] }>("/v1/admin/announcements");
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

export function fetchAnalytics(): Promise<AdminAnalytics> {
  return request<AdminAnalytics>("/v1/admin/analytics");
}
