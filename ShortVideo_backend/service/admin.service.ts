import { AppError } from "../middleware/errorHandler";
import {
  createAnnouncement as createAnnouncementRepo,
  deleteAnnouncement as deleteAnnouncementRepo,
  deleteAnnouncementInboxCopies,
  fanOutAnnouncementToInbox,
  findAnnouncementById,
  findAnnouncements,
  findReportsPage,
  findUsersPage,
  findVideosPage,
  getAnalyticsSnapshot,
  updateAnnouncement as updateAnnouncementRepo,
  updateAnnouncementInboxCopies,
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
  AdminUpdateAnnouncementInput,
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

function mapAnnouncement(announcement: {
  id: string;
  title: string;
  body: string;
  isActive: boolean;
  publishedAt: Date | null;
  createdAt: Date;
  createdById: string | null;
}): AdminAnnouncement {
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

  // Phone Inbox reads `/v1/inbox` (notifications), not the admin announcements table.
  if (announcement.isActive) {
    await fanOutAnnouncementToInbox({
      title: announcement.title,
      body: announcement.body,
    });
  }

  return mapAnnouncement(announcement);
}

export async function updateAnnouncement(
  id: string,
  input: AdminUpdateAnnouncementInput,
): Promise<AdminAnnouncement> {
  const existing = await findAnnouncementById(id);
  if (!existing) {
    throw new AppError(404, "ANNOUNCEMENT_NOT_FOUND", "Announcement not found.");
  }

  const nextTitle = input.title ?? existing.title;
  const nextBody = input.body ?? existing.body;
  const nextIsActive = input.isActive ?? existing.isActive;
  const nextPublishedAt =
    input.publishedAt === undefined
      ? existing.publishedAt
      : input.publishedAt
        ? new Date(input.publishedAt)
        : null;

  const wasInactive = !existing.isActive;
  const becameActive = wasInactive && nextIsActive;

  const announcement = await updateAnnouncementRepo(id, {
    title: input.title,
    body: input.body,
    isActive: input.isActive,
    publishedAt:
      input.publishedAt !== undefined
        ? nextPublishedAt
        : becameActive && !existing.publishedAt
          ? new Date()
          : undefined,
  });

  const becameInactive = existing.isActive && !nextIsActive;

  if (becameInactive) {
    await deleteAnnouncementInboxCopies({
      title: existing.title,
      body: existing.body,
    });
  } else if (nextTitle !== existing.title || nextBody !== existing.body) {
    await updateAnnouncementInboxCopies({
      previousTitle: existing.title,
      previousBody: existing.body,
      title: nextTitle,
      body: nextBody,
    });
  }

  // Newly activated announcements should appear in inboxes.
  if (becameActive) {
    await fanOutAnnouncementToInbox({
      title: nextTitle,
      body: nextBody,
    });
  }

  return mapAnnouncement(announcement);
}

export async function deleteAnnouncement(id: string): Promise<void> {
  const existing = await findAnnouncementById(id);
  if (!existing) {
    throw new AppError(404, "ANNOUNCEMENT_NOT_FOUND", "Announcement not found.");
  }

  await deleteAnnouncementInboxCopies({
    title: existing.title,
    body: existing.body,
  });
  await deleteAnnouncementRepo(id);
}

export async function getAnalytics(): Promise<AdminAnalytics> {
  return getAnalyticsSnapshot();
}
