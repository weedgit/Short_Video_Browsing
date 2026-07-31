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
  return prisma.announcement.create({ data: params });
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
