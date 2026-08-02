import { AppError } from "../middleware/errorHandler";
import {
  countAnnouncements,
  countReports,
  countUserFollowers,
  countUserFollowing,
  countUserVideos,
  countUsers,
  countVideos,
  createAnnouncement as createAnnouncementRepo,
  deleteAnnouncement as deleteAnnouncementRepo,
  deleteAnnouncementInboxCopies,
  fanOutAnnouncementToInbox,
  findAdminUserById,
  findAdminVideoById,
  findAnnouncementById,
  findAnnouncementsPage,
  findReportsPage,
  findUsersPage,
  findVideosPage,
  getAnalyticsDailySeries,
  getAnalyticsPeriodTotals,
  getAnalyticsSnapshot,
  sumUserVideoLikes,
  updateAnnouncement as updateAnnouncementRepo,
  updateAnnouncementInboxCopies,
  updateReportStatus as updateReportStatusRepo,
  updateUserAdminFields,
  updateVideoStatus as updateVideoStatusRepo,
} from "../db/repositories/admin.repository";
import { resolveSignedPlaybackUrl, resolveThumbnailUrl } from "../integrations/cloudflare";
import type {
  AdminAnalytics,
  AdminAnnouncement,
  AdminAnnouncementsPage,
  AdminReportsPage,
  AdminUserProfile,
  AdminUserSummary,
  AdminUsersPage,
  AdminVideoSummary,
  AdminVideosPage,
} from "../models/admin.types";
import type {
  AdminAnnouncementsQueryInput,
  AdminCreateAnnouncementInput,
  AdminReportsQueryInput,
  AdminUpdateAnnouncementInput,
  AdminUpdateReportInput,
  AdminUpdateUserInput,
  AdminUpdateVideoInput,
  AdminUsersQueryInput,
  AdminVideosQueryInput,
} from "../validators/admin.schema";

function pageMeta(total: number, page: number, limit: number) {
  const totalPages = Math.max(1, Math.ceil(total / limit) || 1);
  const safePage = Math.min(page, totalPages);
  return {
    page: safePage,
    limit,
    total,
    totalPages,
    hasMore: safePage < totalPages,
  };
}

export async function listUsers(query: AdminUsersQueryInput): Promise<AdminUsersPage> {
  const filter = { q: query.q, role: query.role, status: query.status };
  const skip = (query.page - 1) * query.limit;
  const [rows, total] = await Promise.all([
    findUsersPage({ ...filter, limit: query.limit, skip }),
    countUsers(filter),
  ]);

  const items: AdminUserSummary[] = rows.map((user) => ({
    id: user.id,
    email: user.email,
    username: user.username,
    displayName: user.displayName,
    avatarUrl: user.avatarUrl,
    bio: user.bio,
    role: user.role,
    status: user.status,
    createdAt: user.createdAt.toISOString(),
  }));

  return { items, ...pageMeta(total, query.page, query.limit) };
}

export async function updateUser(userId: string, input: AdminUpdateUserInput): Promise<AdminUserSummary> {
  const user = await updateUserAdminFields(userId, { status: input.status, role: input.role });

  return {
    id: user.id,
    email: user.email,
    username: user.username,
    displayName: user.displayName,
    avatarUrl: user.avatarUrl,
    bio: user.bio,
    role: user.role,
    status: user.status,
    createdAt: user.createdAt.toISOString(),
  };
}

function mapAdminVideo(video: {
  id: string;
  userId: string;
  description: string;
  status: string;
  durationMs: number;
  category: string | null;
  likeCount: number;
  commentCount: number;
  shareCount: number;
  musicLabel: string | null;
  createdAt: Date;
  cloudflareAssetId: string | null;
  hlsUrl: string | null;
  streamUrl: string | null;
  thumbnailUrl: string | null;
  user: {
    displayName: string;
    username: string;
    avatarUrl: string | null;
  };
  hashtags: Array<{ tag: string }>;
}): AdminVideoSummary {
  let streamUrl: string | null = null;
  let playbackFormat: "hls" | "mp4" | null = null;
  try {
    const signed = resolveSignedPlaybackUrl({
      cloudflareAssetId: video.cloudflareAssetId,
      hlsUrl: video.hlsUrl,
      streamUrl: video.streamUrl,
    });
    streamUrl = signed.url;
    playbackFormat = signed.format;
  } catch {
    // Keep null when playback URL cannot be resolved.
  }

  return {
    id: video.id,
    userId: video.userId,
    authorName: video.user.displayName,
    authorUsername: video.user.username,
    authorAvatarUrl: video.user.avatarUrl,
    description: video.description,
    status: video.status,
    thumbnailUrl: resolveThumbnailUrl(video),
    streamUrl,
    playbackFormat,
    likeCount: video.likeCount,
    commentCount: video.commentCount,
    shareCount: video.shareCount,
    durationMs: video.durationMs,
    category: video.category,
    hashtags: video.hashtags.map((row) => row.tag),
    musicLabel: video.musicLabel,
    createdAt: video.createdAt.toISOString(),
  };
}

export async function listVideos(query: AdminVideosQueryInput): Promise<AdminVideosPage> {
  const filter = {
    status: query.status,
    q: query.q,
    hashtag: query.hashtag,
    category: query.category,
  };
  const skip = (query.page - 1) * query.limit;
  const [rows, total] = await Promise.all([
    findVideosPage({ ...filter, limit: query.limit, skip }),
    countVideos(filter),
  ]);
  const items = rows.map(mapAdminVideo);
  return { items, ...pageMeta(total, query.page, query.limit) };
}

export async function getVideo(videoId: string): Promise<AdminVideoSummary> {
  const video = await findAdminVideoById(videoId);
  if (!video) {
    throw new AppError(404, "VIDEO_NOT_FOUND", "Video not found.");
  }
  return mapAdminVideo(video);
}

export async function getUserProfile(userId: string): Promise<AdminUserProfile> {
  const user = await findAdminUserById(userId);
  if (!user) {
    throw new AppError(404, "USER_NOT_FOUND", "User not found.");
  }

  const [followerCount, followingCount, videoCount, likeCount] = await Promise.all([
    countUserFollowers(userId),
    countUserFollowing(userId),
    countUserVideos(userId),
    sumUserVideoLikes(userId),
  ]);

  return {
    id: user.id,
    email: user.email,
    username: user.username,
    displayName: user.displayName,
    avatarUrl: user.avatarUrl,
    bio: user.bio,
    role: user.role,
    status: user.status,
    createdAt: user.createdAt.toISOString(),
    followerCount,
    followingCount,
    videoCount,
    likeCount,
  };
}

export async function updateVideoStatus(videoId: string, input: AdminUpdateVideoInput) {
  const video = await updateVideoStatusRepo(videoId, input.status);
  return { id: video.id, status: video.status };
}

function parseReportReason(reason: string): { title: string; message: string } {
  const trimmed = reason.trim();
  if (!trimmed) return { title: "(no title)", message: "" };

  const doubleBreak = trimmed.split(/\n\n+/);
  if (doubleBreak.length >= 2) {
    return {
      title: doubleBreak[0]!.trim() || "(no title)",
      message: doubleBreak.slice(1).join("\n\n").trim(),
    };
  }

  const lines = trimmed.split("\n");
  if (lines.length >= 2) {
    return {
      title: lines[0]!.trim() || "(no title)",
      message: lines.slice(1).join("\n").trim(),
    };
  }

  return { title: trimmed, message: "" };
}

export async function listReports(query: AdminReportsQueryInput): Promise<AdminReportsPage> {
  const filter = { status: query.status, q: query.q };
  const skip = (query.page - 1) * query.limit;
  const [rows, total] = await Promise.all([
    findReportsPage({ ...filter, limit: query.limit, skip }),
    countReports(filter),
  ]);

  const items = rows.map((report) => {
    const { title, message } = parseReportReason(report.reason);
    return {
      id: report.id,
      reporterId: report.reporterId,
      reporterName: report.reporter.displayName,
      targetType: report.targetType,
      targetId: report.targetId,
      reason: report.reason,
      title,
      message,
      status: report.status,
      createdAt: report.createdAt.toISOString(),
      resolvedAt: report.resolvedAt ? report.resolvedAt.toISOString() : null,
    };
  });

  return { items, ...pageMeta(total, query.page, query.limit) };
}

export async function updateReport(id: string, input: AdminUpdateReportInput) {
  const report = await updateReportStatusRepo(id, input.status);
  return {
    id: report.id,
    status: report.status,
    resolvedAt: report.resolvedAt ? report.resolvedAt.toISOString() : null,
  };
}

export async function listAnnouncements(
  query: AdminAnnouncementsQueryInput,
): Promise<AdminAnnouncementsPage> {
  const filter = {
    q: query.q,
    isActive: query.active === "true" ? true : query.active === "false" ? false : undefined,
  };
  const skip = (query.page - 1) * query.limit;
  const [rows, total] = await Promise.all([
    findAnnouncementsPage({ ...filter, limit: query.limit, skip }),
    countAnnouncements(filter),
  ]);
  const items = rows.map(mapAnnouncement);
  return { items, ...pageMeta(total, query.page, query.limit) };
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

export async function getAnalytics(rangeDays = 7): Promise<AdminAnalytics> {
  const days = rangeDays === 30 ? 30 : 7;
  const currentStart = startOfUtcDay(daysAgoUtc(days - 1));
  const nextDay = startOfUtcDay(daysAgoUtc(-1));
  const previousStart = startOfUtcDay(daysAgoUtc(days * 2 - 1));
  const previousEnd = currentStart;

  const [snapshot, series, currentTotals, previousTotals] = await Promise.all([
    getAnalyticsSnapshot(),
    getAnalyticsDailySeries(days),
    getAnalyticsPeriodTotals(currentStart, nextDay),
    getAnalyticsPeriodTotals(previousStart, previousEnd),
  ]);

  return {
    ...snapshot,
    rangeDays: days,
    series,
    trends: {
      users: buildTrend(currentTotals.users, previousTotals.users),
      videos: buildTrend(currentTotals.videos, previousTotals.videos),
      likes: buildTrend(currentTotals.likes, previousTotals.likes),
      comments: buildTrend(currentTotals.comments, previousTotals.comments),
      reports: buildTrend(currentTotals.reports, previousTotals.reports),
    },
  };
}

function buildTrend(currentPeriodTotal: number, previousPeriodTotal: number) {
  let changePercent: number | null = null;
  if (previousPeriodTotal > 0) {
    changePercent =
      Math.round(((currentPeriodTotal - previousPeriodTotal) / previousPeriodTotal) * 1000) / 10;
  } else if (currentPeriodTotal > 0) {
    changePercent = 100;
  } else {
    changePercent = 0;
  }

  const direction =
    currentPeriodTotal > previousPeriodTotal
      ? ("up" as const)
      : currentPeriodTotal < previousPeriodTotal
        ? ("down" as const)
        : ("flat" as const);

  return {
    currentPeriodTotal,
    previousPeriodTotal,
    changePercent,
    direction,
  };
}

function daysAgoUtc(days: number): Date {
  const d = new Date();
  d.setUTCHours(0, 0, 0, 0);
  d.setUTCDate(d.getUTCDate() - days);
  return d;
}

function startOfUtcDay(date: Date): Date {
  const d = new Date(date);
  d.setUTCHours(0, 0, 0, 0);
  return d;
}
