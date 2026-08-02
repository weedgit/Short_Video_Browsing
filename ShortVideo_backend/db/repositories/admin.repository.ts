import type { Prisma, ReportStatus, UserRole, UserStatus, VideoStatus } from "@prisma/client";
import { getPrismaClient } from "../client";

type CursorParam = { createdAt: Date; id: string };

function cursorFilter(cursor?: CursorParam) {
  if (!cursor) return {};
  return {
    OR: [
      { createdAt: { lt: cursor.createdAt } },
      { createdAt: cursor.createdAt, id: { lt: cursor.id } },
    ],
  };
}

export async function findUsersPage(params: { limit: number; cursor?: CursorParam }) {
  const prisma = getPrismaClient();
  return prisma.user.findMany({
    where: cursorFilter(params.cursor),
    orderBy: [{ createdAt: "desc" }, { id: "desc" }],
    take: params.limit,
  });
}

export async function updateUserAdminFields(
  userId: string,
  data: { status?: UserStatus; role?: UserRole },
) {
  const prisma = getPrismaClient();
  return prisma.user.update({
    where: { id: userId },
    data: {
      status: data.status,
      role: data.role,
    },
  });
}

export async function findVideosPage(params: {
  status?: VideoStatus;
  limit: number;
  cursor?: CursorParam;
}) {
  const prisma = getPrismaClient();
  return prisma.video.findMany({
    where: {
      ...(params.status ? { status: params.status } : {}),
      ...cursorFilter(params.cursor),
    },
    include: { user: true },
    orderBy: [{ createdAt: "desc" }, { id: "desc" }],
    take: params.limit,
  });
}

export async function updateVideoStatus(videoId: string, status: VideoStatus) {
  const prisma = getPrismaClient();
  return prisma.video.update({ where: { id: videoId }, data: { status } });
}

export async function findReportsPage(params: {
  status?: ReportStatus;
  limit: number;
  cursor?: CursorParam;
}) {
  const prisma = getPrismaClient();
  return prisma.report.findMany({
    where: {
      ...(params.status ? { status: params.status } : {}),
      ...cursorFilter(params.cursor),
    },
    include: { reporter: true },
    orderBy: [{ createdAt: "desc" }, { id: "desc" }],
    take: params.limit,
  });
}

export async function updateReportStatus(id: string, status: ReportStatus) {
  const prisma = getPrismaClient();
  return prisma.report.update({
    where: { id },
    data: {
      status,
      resolvedAt: status === "OPEN" ? null : new Date(),
    },
  });
}

export async function findAnnouncements(limit: number) {
  const prisma = getPrismaClient();
  return prisma.announcement.findMany({
    orderBy: { createdAt: "desc" },
    take: limit,
  });
}

export async function createAnnouncement(params: {
  title: string;
  body: string;
  createdById?: string;
  publishedAt?: Date;
  isActive: boolean;
}) {
  const prisma = getPrismaClient();
  return prisma.announcement.create({
    data: {
      ...params,
      publishedAt: params.publishedAt ?? (params.isActive ? new Date() : undefined),
    },
  });
}

/** Fan-out an announcement into each active user's inbox (`notifications`). */
export async function fanOutAnnouncementToInbox(params: {
  title: string;
  body: string;
  createdAt?: Date;
}): Promise<number> {
  const prisma = getPrismaClient();
  const users = await prisma.user.findMany({
    where: { status: "ACTIVE", deletedAt: null },
    select: { id: true },
  });

  if (users.length === 0) {
    return 0;
  }

  const result = await prisma.notification.createMany({
    data: users.map((user) => ({
      userId: user.id,
      type: "ANNOUNCEMENT",
      title: params.title,
      body: params.body,
      isRead: false,
      ...(params.createdAt ? { createdAt: params.createdAt } : {}),
    })),
  });

  return result.count;
}

/**
 * Ensure every active announcement appears in this user's inbox.
 * Covers seed data and announcements created before fan-out existed.
 */
export async function ensureActiveAnnouncementsForUser(userId: string): Promise<number> {
  const prisma = getPrismaClient();
  const active = await prisma.announcement.findMany({
    where: { isActive: true },
    select: { title: true, body: true, publishedAt: true, createdAt: true },
  });
  if (active.length === 0) {
    return 0;
  }

  const existing = await prisma.notification.findMany({
    where: { userId, type: "ANNOUNCEMENT" },
    select: { title: true, body: true },
  });
  const existingKeys = new Set(existing.map((row) => `${row.title}\n${row.body}`));
  const missing = active.filter((row) => !existingKeys.has(`${row.title}\n${row.body}`));
  if (missing.length === 0) {
    return 0;
  }

  const result = await prisma.notification.createMany({
    data: missing.map((row) => ({
      userId,
      type: "ANNOUNCEMENT",
      title: row.title,
      body: row.body,
      isRead: false,
      createdAt: row.publishedAt ?? row.createdAt,
    })),
  });
  return result.count;
}

export async function findAnnouncementById(id: string) {
  const prisma = getPrismaClient();
  return prisma.announcement.findUnique({ where: { id } });
}

export async function updateAnnouncement(
  id: string,
  data: {
    title?: string;
    body?: string;
    publishedAt?: Date | null;
    isActive?: boolean;
  },
) {
  const prisma = getPrismaClient();
  return prisma.announcement.update({
    where: { id },
    data,
  });
}

export async function deleteAnnouncement(id: string): Promise<void> {
  const prisma = getPrismaClient();
  await prisma.announcement.delete({ where: { id } });
}

/** Keep inbox copies in sync when an announcement title/body changes. */
export async function updateAnnouncementInboxCopies(params: {
  previousTitle: string;
  previousBody: string;
  title: string;
  body: string;
}): Promise<number> {
  const prisma = getPrismaClient();
  const result = await prisma.notification.updateMany({
    where: {
      type: "ANNOUNCEMENT",
      title: params.previousTitle,
      body: params.previousBody,
    },
    data: {
      title: params.title,
      body: params.body,
    },
  });
  return result.count;
}

export async function deleteAnnouncementInboxCopies(params: {
  title: string;
  body: string;
}): Promise<number> {
  const prisma = getPrismaClient();
  const result = await prisma.notification.deleteMany({
    where: {
      type: "ANNOUNCEMENT",
      title: params.title,
      body: params.body,
    },
  });
  return result.count;
}

export async function getAnalyticsSnapshot(): Promise<{
  userCount: number;
  videoCount: number;
  readyVideoCount: number;
  openReportCount: number;
  likeCount: number;
  commentCount: number;
}> {
  const prisma = getPrismaClient();
  const [userCount, videoCount, readyVideoCount, openReportCount, likeCount, commentCount] =
    await Promise.all([
      prisma.user.count(),
      prisma.video.count(),
      prisma.video.count({ where: { status: "READY" } }),
      prisma.report.count({ where: { status: "OPEN" } }),
      prisma.videoLike.count(),
      prisma.videoComment.count(),
    ]);

  return { userCount, videoCount, readyVideoCount, openReportCount, likeCount, commentCount };
}

export type { Prisma };
