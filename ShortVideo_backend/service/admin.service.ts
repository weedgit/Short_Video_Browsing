import { AppError } from "../middleware/errorHandler";
import {
  createAnnouncement as createAnnouncementRepo,
  findAnnouncements,
  findReportsPage,
  findUsersPage,
  findVideosPage,
  getAnalyticsSnapshot,
  updateReportStatus as updateReportStatusRepo,
  updateUserAdminFields,
  updateVideoStatus as updateVideoStatusRepo,
} from "../db/repositories/admin.repository";
import { resolveThumbnailUrl } from "../integrations/cloudflare";
import type {
  AdminAnalytics,
  AdminAnnouncement,
  AdminReportsPage,
  AdminUserSummary,
  AdminUsersPage,
  AdminVideosPage,
} from "../models/admin.types";
import { decodeFeedCursor, encodeFeedCursor } from "../utils/feedCursor";
import type {
  AdminCreateAnnouncementInput,
  AdminReportsQueryInput,
  AdminUpdateReportInput,
  AdminUpdateUserInput,
  AdminUpdateVideoInput,
  AdminUsersQueryInput,
  AdminVideosQueryInput,
} from "../validators/admin.schema";

function decodeCursorSafe(cursor?: string): { createdAt: Date; id: string } | undefined {
  if (!cursor) return undefined;
  try {
    const payload = decodeFeedCursor(cursor);
    return { createdAt: new Date(payload.createdAt), id: payload.id };
  } catch {
    throw new AppError(400, "INVALID_CURSOR", "Invalid pagination cursor.");
  }
}

export async function listUsers(query: AdminUsersQueryInput): Promise<AdminUsersPage> {
  const cursor = decodeCursorSafe(query.cursor);
  const rows = await findUsersPage({ limit: query.limit + 1, cursor });
  const hasMore = rows.length > query.limit;
  const page = hasMore ? rows.slice(0, query.limit) : rows;

  const items: AdminUserSummary[] = page.map((user) => ({
    id: user.id,
    email: user.email,
    username: user.username,
    displayName: user.displayName,
    role: user.role,
    status: user.status,
    createdAt: user.createdAt.toISOString(),
  }));

  const last = page.at(-1);
  const nextCursor =
    hasMore && last ? encodeFeedCursor({ createdAt: last.createdAt.toISOString(), id: last.id }) : null;

  return { items, nextCursor, hasMore };
}

export async function updateUser(userId: string, input: AdminUpdateUserInput): Promise<AdminUserSummary> {
  const user = await updateUserAdminFields(userId, { status: input.status, role: input.role });

  return {
    id: user.id,
    email: user.email,
    username: user.username,
    displayName: user.displayName,
    role: user.role,
    status: user.status,
    createdAt: user.createdAt.toISOString(),
  };
}

export async function listVideos(query: AdminVideosQueryInput): Promise<AdminVideosPage> {
  const cursor = decodeCursorSafe(query.cursor);
  const rows = await findVideosPage({ status: query.status, limit: query.limit + 1, cursor });
  const hasMore = rows.length > query.limit;
  const page = hasMore ? rows.slice(0, query.limit) : rows;

  const items = page.map((video) => ({
    id: video.id,
    userId: video.userId,
    authorName: video.user.displayName,
    description: video.description,
    status: video.status,
    thumbnailUrl: resolveThumbnailUrl(video),
    likeCount: video.likeCount,
    createdAt: video.createdAt.toISOString(),
  }));

  const last = page.at(-1);
  const nextCursor =
    hasMore && last ? encodeFeedCursor({ createdAt: last.createdAt.toISOString(), id: last.id }) : null;

  return { items, nextCursor, hasMore };
}

export async function updateVideoStatus(videoId: string, input: AdminUpdateVideoInput) {
  const video = await updateVideoStatusRepo(videoId, input.status);
  return { id: video.id, status: video.status };
}

export async function listReports(query: AdminReportsQueryInput): Promise<AdminReportsPage> {
  const cursor = decodeCursorSafe(query.cursor);
  const rows = await findReportsPage({ status: query.status, limit: query.limit + 1, cursor });
  const hasMore = rows.length > query.limit;
  const page = hasMore ? rows.slice(0, query.limit) : rows;

  const items = page.map((report) => ({
    id: report.id,
    reporterId: report.reporterId,
    reporterName: report.reporter.displayName,
    targetType: report.targetType,
    targetId: report.targetId,
    reason: report.reason,
    status: report.status,
    createdAt: report.createdAt.toISOString(),
    resolvedAt: report.resolvedAt ? report.resolvedAt.toISOString() : null,
  }));

  const last = page.at(-1);
  const nextCursor =
    hasMore && last ? encodeFeedCursor({ createdAt: last.createdAt.toISOString(), id: last.id }) : null;

  return { items, nextCursor, hasMore };
}

export async function updateReport(id: string, input: AdminUpdateReportInput) {
  const report = await updateReportStatusRepo(id, input.status);
  return {
    id: report.id,
    status: report.status,
    resolvedAt: report.resolvedAt ? report.resolvedAt.toISOString() : null,
  };
}

export async function listAnnouncements(): Promise<AdminAnnouncement[]> {
  const rows = await findAnnouncements(50);
  return rows.map((announcement) => ({
    id: announcement.id,
    title: announcement.title,
    body: announcement.body,
    isActive: announcement.isActive,
    publishedAt: announcement.publishedAt ? announcement.publishedAt.toISOString() : null,
    createdAt: announcement.createdAt.toISOString(),
    createdById: announcement.createdById,
  }));
}

export async function createAnnouncement(
  createdById: string,
  input: AdminCreateAnnouncementInput,
): Promise<AdminAnnouncement> {
  const announcement = await createAnnouncementRepo({
    title: input.title,
    body: input.body,
    createdById,
    publishedAt: input.publishedAt ? new Date(input.publishedAt) : undefined,
    isActive: input.isActive,
  });

  return {
    id: announcement.id,
    title: announcement.title,
    body: announcement.body,
    isActive: announcement.isActive,
    publishedAt: announcement.publishedAt ? announcement.publishedAt.toISOString() : null,
    createdAt: announcement.createdAt.toISOString(),
    createdById: announcement.createdById,
  };
}

export async function getAnalytics(): Promise<AdminAnalytics> {
  return getAnalyticsSnapshot();
}
