import { PrismaClient } from "@prisma/client";

const prisma = new PrismaClient();

function n(v: bigint | number | undefined): number {
  return Number(v ?? 0);
}

async function main() {
  const [
    users,
    videos,
    likes,
    comments,
    saves,
    follows,
    hashtags,
    notifications,
    reports,
    announcements,
    uploadSessions,
  ] = await Promise.all([
    prisma.user.count(),
    prisma.video.count(),
    prisma.videoLike.count(),
    prisma.videoComment.count(),
    prisma.videoSave.count(),
    prisma.follow.count(),
    prisma.videoHashtag.count(),
    prisma.notification.count(),
    prisma.report.count(),
    prisma.announcement.count(),
    prisma.uploadSession.count(),
  ]);

  console.log("=== COUNTS ===");
  console.log({
    users,
    videos,
    likes,
    comments,
    saves,
    follows,
    hashtags,
    notifications,
    reports,
    announcements,
    uploadSessions,
  });

  const videosWithoutUser = await prisma.$queryRawUnsafe<Array<{ id: string }>>(`
    SELECT v.id FROM videos v
    LEFT JOIN users u ON u.id = v.user_id
    WHERE u.id IS NULL
  `);

  const q = async (sql: string) =>
    n((await prisma.$queryRawUnsafe<Array<{ c: bigint }>>(sql))[0]?.c);

  const integrity = {
    videosWithoutCreator: videosWithoutUser.length,
    likesBadVideo: await q(`
      SELECT COUNT(*)::bigint AS c FROM video_likes l
      LEFT JOIN videos v ON v.id = l.video_id WHERE v.id IS NULL`),
    likesBadUser: await q(`
      SELECT COUNT(*)::bigint AS c FROM video_likes l
      LEFT JOIN users u ON u.id = l.user_id WHERE u.id IS NULL`),
    commentsBadVideo: await q(`
      SELECT COUNT(*)::bigint AS c FROM video_comments c
      LEFT JOIN videos v ON v.id = c.video_id WHERE v.id IS NULL`),
    commentsBadUser: await q(`
      SELECT COUNT(*)::bigint AS c FROM video_comments c
      LEFT JOIN users u ON u.id = c.user_id WHERE u.id IS NULL`),
    savesBadVideo: await q(`
      SELECT COUNT(*)::bigint AS c FROM video_saves s
      LEFT JOIN videos v ON v.id = s.video_id WHERE v.id IS NULL`),
    savesBadUser: await q(`
      SELECT COUNT(*)::bigint AS c FROM video_saves s
      LEFT JOIN users u ON u.id = s.user_id WHERE u.id IS NULL`),
    followsBadFollower: await q(`
      SELECT COUNT(*)::bigint AS c FROM follows f
      LEFT JOIN users u ON u.id = f.follower_id WHERE u.id IS NULL`),
    followsBadFollowing: await q(`
      SELECT COUNT(*)::bigint AS c FROM follows f
      LEFT JOIN users u ON u.id = f.following_id WHERE u.id IS NULL`),
    selfFollows: await q(`
      SELECT COUNT(*)::bigint AS c FROM follows WHERE follower_id = following_id`),
    hashtagsBadVideo: await q(`
      SELECT COUNT(*)::bigint AS c FROM video_hashtags h
      LEFT JOIN videos v ON v.id = h.video_id WHERE v.id IS NULL`),
    notifBadUser: await q(`
      SELECT COUNT(*)::bigint AS c FROM notifications n
      LEFT JOIN users u ON u.id = n.user_id WHERE u.id IS NULL`),
    notifBadActor: await q(`
      SELECT COUNT(*)::bigint AS c FROM notifications n
      LEFT JOIN users u ON u.id = n.actor_user_id
      WHERE n.actor_user_id IS NOT NULL AND u.id IS NULL`),
    notifBadVideo: await q(`
      SELECT COUNT(*)::bigint AS c FROM notifications n
      LEFT JOIN videos v ON v.id = n.video_id
      WHERE n.video_id IS NOT NULL AND v.id IS NULL`),
    uploadsBadVideo: await q(`
      SELECT COUNT(*)::bigint AS c FROM upload_sessions s
      LEFT JOIN videos v ON v.id = s.video_id WHERE v.id IS NULL`),
    uploadsBadUser: await q(`
      SELECT COUNT(*)::bigint AS c FROM upload_sessions s
      LEFT JOIN users u ON u.id = s.user_id WHERE u.id IS NULL`),
    readyVideosMissingStreamUrl: await prisma.video.count({
      where: { status: "READY", OR: [{ streamUrl: null }, { streamUrl: "" }] },
    }),
    videosOwnedByDeletedUsers: await q(`
      SELECT COUNT(*)::bigint AS c FROM videos v
      JOIN users u ON u.id = v.user_id
      WHERE u.deleted_at IS NOT NULL OR u.status = 'DELETED'`),
    duplicateLikes: await q(`
      SELECT COUNT(*)::bigint AS c FROM (
        SELECT video_id, user_id FROM video_likes GROUP BY video_id, user_id HAVING COUNT(*) > 1
      ) t`),
  };

  console.log("\n=== ORPHAN / FK INTEGRITY ===");
  console.log(integrity);

  const likeCounterMismatches = await prisma.$queryRawUnsafe<
    Array<{ id: string; like_count: number; actual: bigint }>
  >(`
    SELECT v.id, v.like_count, COUNT(l.id)::bigint AS actual
    FROM videos v
    LEFT JOIN video_likes l ON l.video_id = v.id
    GROUP BY v.id, v.like_count
    HAVING v.like_count <> COUNT(l.id)
    ORDER BY v.id
    LIMIT 30
  `);

  const commentCounterMismatches = await prisma.$queryRawUnsafe<
    Array<{ id: string; comment_count: number; actual: bigint }>
  >(`
    SELECT v.id, v.comment_count, COUNT(c.id)::bigint AS actual
    FROM videos v
    LEFT JOIN video_comments c ON c.video_id = v.id
    GROUP BY v.id, v.comment_count
    HAVING v.comment_count <> COUNT(c.id)
    ORDER BY v.id
    LIMIT 30
  `);

  console.log("\n=== COUNTER MISMATCHES ===");
  console.log({
    likeMismatchCount: likeCounterMismatches.length,
    likeCounterMismatches: likeCounterMismatches.map((r) => ({
      id: r.id,
      stored: r.like_count,
      actual: n(r.actual),
    })),
    commentMismatchCount: commentCounterMismatches.length,
    commentCounterMismatches: commentCounterMismatches.map((r) => ({
      id: r.id,
      stored: r.comment_count,
      actual: n(r.actual),
    })),
  });

  const creators = await prisma.$queryRawUnsafe<
    Array<{
      username: string;
      email: string;
      video_count: bigint;
      like_sum: bigint;
      follower_count: bigint;
      following_count: bigint;
    }>
  >(`
    SELECT u.username, u.email,
      (SELECT COUNT(*) FROM videos v WHERE v.user_id = u.id)::bigint AS video_count,
      (SELECT COALESCE(SUM(v.like_count),0) FROM videos v WHERE v.user_id = u.id)::bigint AS like_sum,
      (SELECT COUNT(*) FROM follows f WHERE f.following_id = u.id)::bigint AS follower_count,
      (SELECT COUNT(*) FROM follows f WHERE f.follower_id = u.id)::bigint AS following_count
    FROM users u
    ORDER BY u.created_at
  `);

  console.log("\n=== USERS / GRAPH ===");
  for (const u of creators) {
    console.log(
      `${u.username.padEnd(18)} videos=${String(u.video_count).padStart(3)} likeSum=${String(u.like_sum).padStart(3)} followers=${u.follower_count} following=${u.following_count}  ${u.email}`,
    );
  }

  const sampleVideos = await prisma.video.findMany({
    take: 8,
    orderBy: { createdAt: "desc" },
    include: {
      user: { select: { username: true } },
      _count: { select: { likes: true, comments: true, saves: true, hashtags: true } },
    },
  });

  console.log("\n=== SAMPLE VIDEOS ===");
  for (const v of sampleVideos) {
    console.log({
      id: v.id.slice(0, 12),
      creator: v.user.username,
      status: v.status,
      hasStream: Boolean(v.streamUrl),
      like_count: v.likeCount,
      likes_actual: v._count.likes,
      comment_count: v.commentCount,
      comments_actual: v._count.comments,
      saves: v._count.saves,
      hashtags: v._count.hashtags,
    });
  }

  const allReports = await prisma.report.findMany();
  let badReports = 0;
  for (const r of allReports) {
    if (r.targetType === "VIDEO") {
      if (!(await prisma.video.findUnique({ where: { id: r.targetId }, select: { id: true } }))) {
        badReports += 1;
      }
    } else if (r.targetType === "USER") {
      if (!(await prisma.user.findUnique({ where: { id: r.targetId }, select: { id: true } }))) {
        badReports += 1;
      }
    } else if (r.targetType === "COMMENT") {
      if (
        !(await prisma.videoComment.findUnique({
          where: { id: r.targetId },
          select: { id: true },
        }))
      ) {
        badReports += 1;
      }
    }
  }

  console.log("\n=== REPORTS ===");
  console.log({ totalReports: allReports.length, reportsWithMissingTarget: badReports });

  const orphanProblems = Object.entries(integrity).filter(([, value]) => value > 0);
  console.log("\n=== VERDICT ===");
  if (
    orphanProblems.length === 0 &&
    likeCounterMismatches.length === 0 &&
    commentCounterMismatches.length === 0 &&
    badReports === 0
  ) {
    console.log("OK: no orphan rows; creators/comments/likes/follows all match.");
  } else {
    console.log("ISSUES FOUND:");
    for (const [key, value] of orphanProblems) {
      console.log(` - ${key}: ${value}`);
    }
    if (likeCounterMismatches.length) {
      console.log(` - like counter mismatches: ${likeCounterMismatches.length}`);
    }
    if (commentCounterMismatches.length) {
      console.log(` - comment counter mismatches: ${commentCounterMismatches.length}`);
    }
    if (badReports) console.log(` - reports with missing target: ${badReports}`);
  }
}

main()
  .catch((error) => {
    console.error(error);
    process.exitCode = 1;
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
