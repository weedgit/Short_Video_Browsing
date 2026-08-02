import type { Prisma, ReportStatus, UserRole, UserStatus, VideoStatus } from "@prisma/client";
import { getPrismaClient } from "../client";

function buildUserWhere(params: {
  q?: string;
  role?: UserRole;
  status?: UserStatus;
}): Prisma.UserWhereInput {
  const andFilters: Prisma.UserWhereInput[] = [];
  if (params.role) andFilters.push({ role: params.role });
  if (params.status) andFilters.push({ status: params.status });
  const q = params.q?.trim();
  if (q) {
    const bare = q.replace(/^@/, "");
    andFilters.push({
      OR: [
        { displayName: { contains: q, mode: "insensitive" } },
        { username: { contains: bare, mode: "insensitive" } },
        { email: { contains: q, mode: "insensitive" } },
      ],
    });
  }
  return andFilters.length > 0 ? { AND: andFilters } : {};
}

export async function findUsersPage(params: {
  q?: string;
  role?: UserRole;
  status?: UserStatus;
  limit: number;
  skip: number;
}) {
  const prisma = getPrismaClient();
  return prisma.user.findMany({
    where: buildUserWhere(params),
    orderBy: [{ createdAt: "desc" }, { id: "desc" }],
    skip: params.skip,
    take: params.limit,
  });
}

export async function countUsers(params: {
  q?: string;
  role?: UserRole;
  status?: UserStatus;
}) {
  const prisma = getPrismaClient();
  return prisma.user.count({ where: buildUserWhere(params) });
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
  q?: string;
  hashtag?: string;
  category?: string;
  limit: number;
  skip: number;
}) {
  const prisma = getPrismaClient();
  return prisma.video.findMany({
    where: buildVideoWhere(params),
    include: { user: true, hashtags: true },
    orderBy: [{ createdAt: "desc" }, { id: "desc" }],
    skip: params.skip,
    take: params.limit,
  });
}

export async function countVideos(params: {
  status?: VideoStatus;
  q?: string;
  hashtag?: string;
  category?: string;
}) {
  const prisma = getPrismaClient();
  return prisma.video.count({ where: buildVideoWhere(params) });
}

function buildVideoWhere(params: {
  status?: VideoStatus;
  q?: string;
  hashtag?: string;
  category?: string;
}): Prisma.VideoWhereInput {
  const andFilters: Prisma.VideoWhereInput[] = [];
  if (params.status) andFilters.push({ status: params.status });
  const category = params.category?.trim();
  if (category) andFilters.push({ category: { equals: category, mode: "insensitive" } });
  const hashtag = params.hashtag?.trim().replace(/^#/, "");
  if (hashtag) {
    andFilters.push({
      hashtags: { some: { tag: { contains: hashtag, mode: "insensitive" } } },
    });
  }
  const q = params.q?.trim();
  if (q) {
    const bare = q.replace(/^#/, "");
    andFilters.push({
      OR: [
        { description: { contains: q, mode: "insensitive" } },
        { category: { contains: q, mode: "insensitive" } },
        { user: { displayName: { contains: q, mode: "insensitive" } } },
        { user: { username: { contains: q, mode: "insensitive" } } },
        { hashtags: { some: { tag: { contains: bare, mode: "insensitive" } } } },
      ],
    });
  }
  return andFilters.length > 0 ? { AND: andFilters } : {};
}

export async function findAdminVideoById(videoId: string) {
  const prisma = getPrismaClient();
  return prisma.video.findUnique({
    where: { id: videoId },
    include: { user: true, hashtags: true },
  });
}

export async function findAdminUserById(userId: string) {
  const prisma = getPrismaClient();
  return prisma.user.findUnique({ where: { id: userId } });
}

export async function countUserFollowers(userId: string) {
  const prisma = getPrismaClient();
  return prisma.follow.count({ where: { followingId: userId } });
}

export async function countUserFollowing(userId: string) {
  const prisma = getPrismaClient();
  return prisma.follow.count({ where: { followerId: userId } });
}

export async function countUserVideos(userId: string) {
  const prisma = getPrismaClient();
  return prisma.video.count({
    where: { userId, status: "READY", deletedAt: null },
  });
}

export async function sumUserVideoLikes(userId: string) {
  const prisma = getPrismaClient();
  const result = await prisma.video.aggregate({
    where: { userId, deletedAt: null },
    _sum: { likeCount: true },
  });
  return result._sum.likeCount ?? 0;
}

export async function updateVideoStatus(videoId: string, status: VideoStatus) {
  const prisma = getPrismaClient();
  return prisma.video.update({ where: { id: videoId }, data: { status } });
}

export async function findReportsPage(params: {
  status?: ReportStatus;
  q?: string;
  limit: number;
  skip: number;
}) {
  const prisma = getPrismaClient();
  return prisma.report.findMany({
    where: buildReportWhere(params),
    include: { reporter: true },
    orderBy: [{ createdAt: "desc" }, { id: "desc" }],
    skip: params.skip,
    take: params.limit,
  });
}

export async function countReports(params: { status?: ReportStatus; q?: string }) {
  const prisma = getPrismaClient();
  return prisma.report.count({ where: buildReportWhere(params) });
}

function buildReportWhere(params: {
  status?: ReportStatus;
  q?: string;
}): Prisma.ReportWhereInput {
  const andFilters: Prisma.ReportWhereInput[] = [];
  if (params.status) andFilters.push({ status: params.status });
  const q = params.q?.trim();
  if (q) {
    const upper = q.toUpperCase();
    const targetType =
      upper === "VIDEO" || upper === "USER" || upper === "COMMENT"
        ? (upper as "VIDEO" | "USER" | "COMMENT")
        : undefined;
    andFilters.push({
      OR: [
        { reason: { contains: q, mode: "insensitive" } },
        { targetId: { contains: q, mode: "insensitive" } },
        ...(targetType ? [{ targetType }] : []),
        { reporter: { displayName: { contains: q, mode: "insensitive" } } },
        { reporter: { username: { contains: q, mode: "insensitive" } } },
        { reporter: { email: { contains: q, mode: "insensitive" } } },
      ],
    });
  }
  return andFilters.length > 0 ? { AND: andFilters } : {};
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

export async function findAnnouncementsPage(params: {
  q?: string;
  isActive?: boolean;
  limit: number;
  skip: number;
}) {
  const prisma = getPrismaClient();
  return prisma.announcement.findMany({
    where: buildAnnouncementWhere(params),
    orderBy: [{ createdAt: "desc" }, { id: "desc" }],
    skip: params.skip,
    take: params.limit,
  });
}

export async function countAnnouncements(params: { q?: string; isActive?: boolean }) {
  const prisma = getPrismaClient();
  return prisma.announcement.count({ where: buildAnnouncementWhere(params) });
}

function buildAnnouncementWhere(params: {
  q?: string;
  isActive?: boolean;
}): Prisma.AnnouncementWhereInput {
  const andFilters: Prisma.AnnouncementWhereInput[] = [];
  if (params.isActive !== undefined) andFilters.push({ isActive: params.isActive });
  const q = params.q?.trim();
  if (q) {
    andFilters.push({
      OR: [
        { title: { contains: q, mode: "insensitive" } },
        { body: { contains: q, mode: "insensitive" } },
      ],
    });
  }
  return andFilters.length > 0 ? { AND: andFilters } : {};
}

/** @deprecated Prefer findAnnouncementsPage for paginated admin lists. */
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

type DailyCountRow = {
  day: Date;
  count: number;
};

async function queryDailyCounts(table: string, since: Date): Promise<DailyCountRow[]> {
  const prisma = getPrismaClient();
  // Table names are fixed allow-list callers only.
  const allowed = new Set(["users", "videos", "video_likes", "video_comments", "reports"]);
  if (!allowed.has(table)) {
    throw new Error(`Unsupported analytics table: ${table}`);
  }

  return prisma.$queryRawUnsafe<DailyCountRow[]>(
    `
      SELECT DATE_TRUNC('day', created_at)::date AS day, COUNT(*)::int AS count
      FROM ${table}
      WHERE created_at >= $1
      GROUP BY 1
      ORDER BY 1 ASC
    `,
    since,
  );
}

export async function getAnalyticsDailySeries(rangeDays: number): Promise<{
  users: Array<{ date: string; count: number }>;
  videos: Array<{ date: string; count: number }>;
  likes: Array<{ date: string; count: number }>;
  comments: Array<{ date: string; count: number }>;
  reports: Array<{ date: string; count: number }>;
}> {
  const since = startOfUtcDay(daysAgoUtc(rangeDays - 1));
  const [users, videos, likes, comments, reports] = await Promise.all([
    queryDailyCounts("users", since),
    queryDailyCounts("videos", since),
    queryDailyCounts("video_likes", since),
    queryDailyCounts("video_comments", since),
    queryDailyCounts("reports", since),
  ]);

  return {
    users: fillDailySeries(users, rangeDays),
    videos: fillDailySeries(videos, rangeDays),
    likes: fillDailySeries(likes, rangeDays),
    comments: fillDailySeries(comments, rangeDays),
    reports: fillDailySeries(reports, rangeDays),
  };
}

export async function getAnalyticsPeriodTotals(
  startInclusive: Date,
  endExclusive: Date,
): Promise<{
  users: number;
  videos: number;
  likes: number;
  comments: number;
  reports: number;
}> {
  const prisma = getPrismaClient();
  const [users, videos, likes, comments, reports] = await Promise.all([
    prisma.user.count({
      where: { createdAt: { gte: startInclusive, lt: endExclusive } },
    }),
    prisma.video.count({
      where: { createdAt: { gte: startInclusive, lt: endExclusive } },
    }),
    prisma.videoLike.count({
      where: { createdAt: { gte: startInclusive, lt: endExclusive } },
    }),
    prisma.videoComment.count({
      where: { createdAt: { gte: startInclusive, lt: endExclusive } },
    }),
    prisma.report.count({
      where: { createdAt: { gte: startInclusive, lt: endExclusive } },
    }),
  ]);

  return { users, videos, likes, comments, reports };
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

function toDateKey(date: Date): string {
  return date.toISOString().slice(0, 10);
}

function fillDailySeries(
  rows: DailyCountRow[],
  rangeDays: number,
): Array<{ date: string; count: number }> {
  const byDay = new Map<string, number>();
  for (const row of rows) {
    const key = toDateKey(new Date(row.day));
    byDay.set(key, Number(row.count) || 0);
  }

  const points: Array<{ date: string; count: number }> = [];
  for (let i = rangeDays - 1; i >= 0; i -= 1) {
    const day = daysAgoUtc(i);
    const key = toDateKey(day);
    points.push({ date: key, count: byDay.get(key) ?? 0 });
  }
  return points;
}

export type { Prisma };
