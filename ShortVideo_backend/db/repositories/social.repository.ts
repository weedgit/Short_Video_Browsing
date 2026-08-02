import type { Prisma, ReportTargetType } from "@prisma/client";
import { getPrismaClient } from "../client";

type CursorParam = { createdAt: Date; id: string };

// ---------- Likes ----------

export async function likeVideo(videoId: string, userId: string): Promise<{ likeCount: number }> {
  const prisma = getPrismaClient();

  return prisma.$transaction(async (tx) => {
    const existing = await tx.videoLike.findUnique({
      where: { videoId_userId: { videoId, userId } },
    });

    if (!existing) {
      await tx.videoLike.create({ data: { videoId, userId } });
      await tx.video.update({
        where: { id: videoId },
        data: { likeCount: { increment: 1 } },
      });
    }

    const video = await tx.video.findUniqueOrThrow({
      where: { id: videoId },
      select: { likeCount: true },
    });

    return { likeCount: video.likeCount };
  });
}

export async function unlikeVideo(videoId: string, userId: string): Promise<{ likeCount: number }> {
  const prisma = getPrismaClient();

  return prisma.$transaction(async (tx) => {
    const existing = await tx.videoLike.findUnique({
      where: { videoId_userId: { videoId, userId } },
    });

    if (existing) {
      await tx.videoLike.delete({ where: { videoId_userId: { videoId, userId } } });
      await tx.video.update({
        where: { id: videoId },
        data: { likeCount: { decrement: 1 } },
      });
    }

    const video = await tx.video.findUniqueOrThrow({
      where: { id: videoId },
      select: { likeCount: true },
    });

    return { likeCount: Math.max(0, video.likeCount) };
  });
}

export async function findLikedVideoIds(userId: string, videoIds: string[]): Promise<Set<string>> {
  if (videoIds.length === 0) return new Set();
  const prisma = getPrismaClient();
  const rows = await prisma.videoLike.findMany({
    where: { userId, videoId: { in: videoIds } },
    select: { videoId: true },
  });
  return new Set(rows.map((row) => row.videoId));
}

// ---------- Saves ----------

export async function saveVideo(videoId: string, userId: string): Promise<void> {
  const prisma = getPrismaClient();
  await prisma.videoSave.upsert({
    where: { videoId_userId: { videoId, userId } },
    create: { videoId, userId },
    update: {},
  });
}

export async function unsaveVideo(videoId: string, userId: string): Promise<void> {
  const prisma = getPrismaClient();
  await prisma.videoSave.deleteMany({ where: { videoId, userId } });
}

export async function findSavedVideoIds(userId: string, videoIds: string[]): Promise<Set<string>> {
  if (videoIds.length === 0) return new Set();
  const prisma = getPrismaClient();
  const rows = await prisma.videoSave.findMany({
    where: { userId, videoId: { in: videoIds } },
    select: { videoId: true },
  });
  return new Set(rows.map((row) => row.videoId));
}

// ---------- Follows ----------

export async function followUser(followerId: string, followingId: string): Promise<void> {
  const prisma = getPrismaClient();
  await prisma.follow.upsert({
    where: { followerId_followingId: { followerId, followingId } },
    create: { followerId, followingId },
    update: {},
  });
}

export async function unfollowUser(followerId: string, followingId: string): Promise<void> {
  const prisma = getPrismaClient();
  await prisma.follow.deleteMany({ where: { followerId, followingId } });
}

export async function isFollowing(followerId: string, followingId: string): Promise<boolean> {
  const prisma = getPrismaClient();
  const row = await prisma.follow.findUnique({
    where: { followerId_followingId: { followerId, followingId } },
  });
  return Boolean(row);
}

export async function countFollowers(userId: string): Promise<number> {
  const prisma = getPrismaClient();
  return prisma.follow.count({ where: { followingId: userId } });
}

export async function countFollowing(userId: string): Promise<number> {
  const prisma = getPrismaClient();
  return prisma.follow.count({ where: { followerId: userId } });
}

export async function findFollowingUserIds(userId: string): Promise<string[]> {
  const prisma = getPrismaClient();
  const rows = await prisma.follow.findMany({
    where: { followerId: userId },
    select: { followingId: true },
  });
  return rows.map((row) => row.followingId);
}

export async function findFollowingUserIdsSubset(
  userId: string,
  candidateIds: string[],
): Promise<Set<string>> {
  if (candidateIds.length === 0) return new Set();
  const prisma = getPrismaClient();
  const rows = await prisma.follow.findMany({
    where: { followerId: userId, followingId: { in: candidateIds } },
    select: { followingId: true },
  });
  return new Set(rows.map((row) => row.followingId));
}

// ---------- Comments ----------

export async function createComment(params: {
  videoId: string;
  userId: string;
  text: string;
  parentId?: string | null;
}) {
  const prisma = getPrismaClient();
  const [comment] = await prisma.$transaction([
    prisma.videoComment.create({
      data: {
        videoId: params.videoId,
        userId: params.userId,
        text: params.text,
        parentId: params.parentId ?? null,
      },
      include: {
        user: true,
        parent: { include: { user: true } },
      },
    }),
    prisma.video.update({
      where: { id: params.videoId },
      data: { commentCount: { increment: 1 } },
    }),
  ]);
  return comment;
}

export async function findCommentById(commentId: string) {
  const prisma = getPrismaClient();
  return prisma.videoComment.findUnique({
    where: { id: commentId },
    include: { user: true, parent: true },
  });
}

/** Root comments only (parentId is null), newest first. */
export async function findRootCommentsByVideo(
  videoId: string,
  limit: number,
  cursor?: CursorParam,
) {
  const prisma = getPrismaClient();
  return prisma.videoComment.findMany({
    where: {
      videoId,
      parentId: null,
      ...(cursor
        ? {
            OR: [
              { createdAt: { lt: cursor.createdAt } },
              { createdAt: cursor.createdAt, id: { lt: cursor.id } },
            ],
          }
        : {}),
    },
    include: { user: true },
    orderBy: [{ createdAt: "desc" }, { id: "desc" }],
    take: limit,
  });
}

/** Direct replies for the given root comment ids, oldest first (TikTok thread order). */
export async function findRepliesByParentIds(parentIds: string[]) {
  if (parentIds.length === 0) return [];
  const prisma = getPrismaClient();
  return prisma.videoComment.findMany({
    where: { parentId: { in: parentIds } },
    include: {
      user: true,
      parent: { include: { user: true } },
    },
    orderBy: [{ createdAt: "asc" }, { id: "asc" }],
  });
}

/** @deprecated Prefer findRootCommentsByVideo + findRepliesByParentIds */
export async function findCommentsByVideo(
  videoId: string,
  limit: number,
  cursor?: CursorParam,
) {
  return findRootCommentsByVideo(videoId, limit, cursor);
}

// ---------- Videos / profile ----------

export async function findVideoOwner(videoId: string): Promise<{ id: string; userId: string } | null> {
  const prisma = getPrismaClient();
  return prisma.video.findUnique({
    where: { id: videoId },
    select: { id: true, userId: true },
  });
}

export async function findUserPublicProfile(userId: string) {
  const prisma = getPrismaClient();
  return prisma.user.findFirst({
    where: { id: userId, deletedAt: null },
  });
}

export async function countUserVideos(userId: string): Promise<number> {
  const prisma = getPrismaClient();
  return prisma.video.count({ where: { userId, status: "READY", deletedAt: null } });
}

export async function sumUserVideoLikes(userId: string): Promise<number> {
  const prisma = getPrismaClient();
  const result = await prisma.video.aggregate({
    where: { userId, status: "READY", deletedAt: null },
    _sum: { likeCount: true },
  });
  return result._sum.likeCount ?? 0;
}

export async function findUserVideos(userId: string, limit: number, cursor?: CursorParam) {
  const prisma = getPrismaClient();
  return prisma.video.findMany({
    where: {
      userId,
      status: "READY",
      deletedAt: null,
      ...(cursor
        ? {
            OR: [
              { createdAt: { lt: cursor.createdAt } },
              { createdAt: cursor.createdAt, id: { lt: cursor.id } },
            ],
          }
        : {}),
    },
    include: {
      user: true,
      hashtags: true,
    },
    orderBy: [{ createdAt: "desc" }, { id: "desc" }],
    take: limit,
  });
}

export async function findLikedVideos(userId: string, limit: number, cursor?: CursorParam) {
  const prisma = getPrismaClient();
  const rows = await prisma.videoLike.findMany({
    where: {
      userId,
      video: { status: "READY", deletedAt: null },
      ...(cursor
        ? {
            OR: [
              { createdAt: { lt: cursor.createdAt } },
              { createdAt: cursor.createdAt, id: { lt: cursor.id } },
            ],
          }
        : {}),
    },
    orderBy: [{ createdAt: "desc" }, { id: "desc" }],
    take: limit,
    include: {
      video: {
        include: {
          user: true,
          hashtags: true,
        },
      },
    },
  });

  return rows.map((row) => ({
    ...row.video,
    cursorCreatedAt: row.createdAt,
    cursorId: row.id,
  }));
}

export async function findSavedVideos(userId: string, limit: number, cursor?: CursorParam) {
  const prisma = getPrismaClient();
  const rows = await prisma.videoSave.findMany({
    where: {
      userId,
      video: { status: "READY", deletedAt: null },
      ...(cursor
        ? {
            OR: [
              { createdAt: { lt: cursor.createdAt } },
              { createdAt: cursor.createdAt, id: { lt: cursor.id } },
            ],
          }
        : {}),
    },
    orderBy: [{ createdAt: "desc" }, { id: "desc" }],
    take: limit,
    include: {
      video: {
        include: {
          user: true,
          hashtags: true,
        },
      },
    },
  });

  return rows.map((row) => ({
    ...row.video,
    cursorCreatedAt: row.createdAt,
    cursorId: row.id,
  }));
}

// ---------- Discover / search ----------

export async function searchUsers(query: string, limit: number) {
  const prisma = getPrismaClient();
  return prisma.user.findMany({
    where: {
      deletedAt: null,
      status: "ACTIVE",
      OR: [
        { username: { contains: query, mode: "insensitive" } },
        { displayName: { contains: query, mode: "insensitive" } },
      ],
    },
    take: limit,
    orderBy: { createdAt: "desc" },
  });
}

export async function listFollowingUsers(followerId: string, limit: number) {
  const prisma = getPrismaClient();
  const rows = await prisma.follow.findMany({
    where: {
      followerId,
      following: { deletedAt: null, status: "ACTIVE" },
    },
    orderBy: { createdAt: "desc" },
    take: limit,
    include: { following: true },
  });
  return rows.map((row) => row.following);
}

export async function searchFollowingUsers(followerId: string, query: string, limit: number) {
  const prisma = getPrismaClient();
  const rows = await prisma.follow.findMany({
    where: {
      followerId,
      following: {
        deletedAt: null,
        status: "ACTIVE",
        OR: [
          { username: { contains: query, mode: "insensitive" } },
          { displayName: { contains: query, mode: "insensitive" } },
        ],
      },
    },
    orderBy: { createdAt: "desc" },
    take: limit,
    include: { following: true },
  });
  return rows.map((row) => row.following);
}

export async function searchHashtags(query: string, limit: number) {
  const prisma = getPrismaClient();
  const rows = await prisma.videoHashtag.groupBy({
    by: ["tag"],
    where: query ? { tag: { contains: query, mode: "insensitive" } } : undefined,
    _count: { tag: true },
    orderBy: { _count: { tag: "desc" } },
    take: limit,
  });
  return rows.map((row) => ({ tag: row.tag, videoCount: row._count.tag }));
}

export async function findTrendingHashtags(limit: number) {
  return searchHashtags("", limit);
}

export async function searchVideos(query: string, limit: number) {
  const prisma = getPrismaClient();
  return prisma.video.findMany({
    where: {
      status: "READY",
      deletedAt: null,
      description: { contains: query, mode: "insensitive" },
    },
    include: { user: true },
    orderBy: { likeCount: "desc" },
    take: limit,
  });
}

export async function findTrendingVideos(limit: number) {
  const prisma = getPrismaClient();
  return prisma.video.findMany({
    where: { status: "READY", deletedAt: null },
    include: { user: true },
    orderBy: { likeCount: "desc" },
    take: limit,
  });
}

// ---------- Reports ----------

export async function createReport(params: {
  reporterId: string;
  targetType: ReportTargetType;
  targetId: string;
  reason: string;
}) {
  const prisma = getPrismaClient();
  return prisma.report.create({ data: params });
}

// ---------- Notifications ----------

export async function createNotification(params: {
  userId: string;
  type: string;
  title: string;
  body: string;
  videoId?: string;
  actorUserId?: string;
}) {
  const prisma = getPrismaClient();
  return prisma.notification.create({ data: params });
}

export async function findNotificationsForUser(userId: string, limit: number, cursor?: CursorParam) {
  const prisma = getPrismaClient();
  return prisma.notification.findMany({
    where: {
      userId,
      ...(cursor
        ? {
            OR: [
              { createdAt: { lt: cursor.createdAt } },
              { createdAt: cursor.createdAt, id: { lt: cursor.id } },
            ],
          }
        : {}),
    },
    include: { actorUser: true },
    orderBy: [{ createdAt: "desc" }, { id: "desc" }],
    take: limit,
  });
}

export async function countUnreadNotifications(userId: string): Promise<number> {
  const prisma = getPrismaClient();
  return prisma.notification.count({ where: { userId, isRead: false } });
}

export async function markNotificationRead(id: string, userId: string): Promise<void> {
  const prisma = getPrismaClient();
  await prisma.notification.updateMany({ where: { id, userId }, data: { isRead: true } });
}

export async function markAllNotificationsRead(userId: string): Promise<void> {
  const prisma = getPrismaClient();
  await prisma.notification.updateMany({ where: { userId, isRead: false }, data: { isRead: true } });
}

export type { Prisma };
