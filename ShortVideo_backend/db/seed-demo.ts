/**
 * Local/demo database seeder.
 * Safe to re-run: wipes previous @demo.local accounts and recreates them.
 *
 * Usage:
 *   DATABASE_URL=postgresql://shortvideo:shortvideo@localhost:5432/shortvideo?schema=public \
 *     npx tsx db/seed-demo.ts
 */
import { PrismaClient, type User } from "@prisma/client";
import { hashPassword } from "../utils/password";

const prisma = new PrismaClient();

const DEMO_PASSWORD = "Password123!";
const SAMPLE_CLIPS = [
  {
    url: "https://download.samplelib.com/mp4/sample-5s.mp4",
    durationMs: 5_000,
    thumb: "https://picsum.photos/seed/sv1/480/854",
  },
  {
    url: "https://download.samplelib.com/mp4/sample-10s.mp4",
    durationMs: 10_000,
    thumb: "https://picsum.photos/seed/sv2/480/854",
  },
  {
    url: "https://download.samplelib.com/mp4/sample-15s.mp4",
    durationMs: 15_000,
    thumb: "https://picsum.photos/seed/sv3/480/854",
  },
  {
    url: "https://download.samplelib.com/mp4/sample-20s.mp4",
    durationMs: 20_000,
    thumb: "https://picsum.photos/seed/sv4/480/854",
  },
  {
    url: "https://download.samplelib.com/mp4/sample-30s.mp4",
    durationMs: 30_000,
    thumb: "https://picsum.photos/seed/sv5/480/854",
  },
] as const;

const USER_SPECS = [
  {
    email: "admin@demo.local",
    username: "demo_admin",
    displayName: "Demo Admin",
    role: "ADMIN" as const,
    bio: "Platform admin for local testing",
  },
  {
    email: "alice@demo.local",
    username: "demo_alice",
    displayName: "Alice Kim",
    role: "USER" as const,
    bio: "Coffee, cats, and short clips",
  },
  {
    email: "bob@demo.local",
    username: "demo_bob",
    displayName: "Bob Chen",
    role: "USER" as const,
    bio: "Street food hunter",
  },
  {
    email: "carol@demo.local",
    username: "demo_carol",
    displayName: "Carol Park",
    role: "USER" as const,
    bio: "Dance & travel",
  },
  {
    email: "dave@demo.local",
    username: "demo_dave",
    displayName: "Dave Nguyen",
    role: "USER" as const,
    bio: "Tech tips in 15 seconds",
  },
  {
    email: "erin@demo.local",
    username: "demo_erin",
    displayName: "Erin Lopez",
    role: "USER" as const,
    bio: "Pets and comedy",
  },
];

const COMMENT_POOL = [
  "This is so good!",
  "Need part 2 🔥",
  "Haha too real",
  "Where was this filmed?",
  "Saving this for later",
  "Algorithm blessed me today",
  "Teaching me something new",
  "Sound on 🔊",
  "Can you do a tutorial?",
  "Following for more",
];

async function wipeDemoUsers(): Promise<void> {
  const demoUsers = await prisma.user.findMany({
    where: {
      OR: [{ email: { endsWith: "@demo.local" } }, { username: { startsWith: "demo_" } }],
    },
    select: { id: true },
  });
  if (demoUsers.length === 0) return;

  const ids = demoUsers.map((u) => u.id);
  // Cascades remove videos/likes/comments/follows for these users.
  await prisma.user.deleteMany({ where: { id: { in: ids } } });
}

async function createUsers(passwordHash: string): Promise<User[]> {
  const users: User[] = [];
  for (const spec of USER_SPECS) {
    const user = await prisma.user.create({
      data: {
        email: spec.email,
        username: spec.username,
        displayName: spec.displayName,
        bio: spec.bio,
        role: spec.role,
        status: "ACTIVE",
        languageCode: "en",
        countryCode: "CN",
        passwordHash,
        avatarUrl: `https://api.dicebear.com/9.x/thumbs/svg?seed=${encodeURIComponent(spec.username)}`,
      },
    });
    users.push(user);
  }
  return users;
}

async function createVideos(creators: User[]) {
  const videos = [];
  let n = 0;
  for (const creator of creators) {
    // Admin gets fewer clips; regular creators get 4 each.
    const count = creator.role === "ADMIN" ? 2 : 4;
    for (let i = 0; i < count; i += 1) {
      const clip = SAMPLE_CLIPS[n % SAMPLE_CLIPS.length]!;
      const tags = [
        "#demo",
        "#shorts",
        creator.username === "demo_alice" ? "#cats" : "#daily",
      ];
      const video = await prisma.video.create({
        data: {
          userId: creator.id,
          description: `${creator.displayName}'s clip #${i + 1} — local demo content`,
          durationMs: clip.durationMs,
          status: "READY",
          streamUrl: clip.url,
          hlsUrl: clip.url,
          thumbnailUrl: clip.thumb,
          category: ["Comedy", "Food", "Travel", "Tech", "Pets"][n % 5]!,
          musicLabel: ["Original Sound", "Lo-fi Beat", "Trending Audio"][n % 3]!,
          shareCount: (n % 7) + 1,
          createdAt: new Date(Date.now() - n * 45 * 60_000),
          hashtags: {
            create: tags.map((tag) => ({ tag })),
          },
        },
      });
      videos.push(video);
      n += 1;
    }
  }
  return videos;
}

async function createSocialGraph(users: User[], videos: Awaited<ReturnType<typeof createVideos>>) {
  const [admin, alice, bob, carol, dave, erin] = users;
  if (!admin || !alice || !bob || !carol || !dave || !erin) {
    throw new Error("Expected 6 demo users");
  }

  // Follows: everyone follows alice & bob; alice follows carol; dave follows erin; etc.
  const followPairs: Array<[User, User]> = [
    [bob, alice],
    [carol, alice],
    [dave, alice],
    [erin, alice],
    [admin, alice],
    [alice, bob],
    [carol, bob],
    [dave, bob],
    [alice, carol],
    [erin, carol],
    [alice, dave],
    [bob, erin],
  ];
  for (const [follower, following] of followPairs) {
    await prisma.follow.create({
      data: { followerId: follower.id, followingId: following.id },
    });
  }

  // Likes / comments / saves / notifications
  let likeCount = 0;
  let commentCount = 0;
  for (let i = 0; i < videos.length; i += 1) {
    const video = videos[i]!;
    const likers = users.filter((u) => u.id !== video.userId).slice(0, 2 + (i % 3));
    for (const liker of likers) {
      await prisma.videoLike.create({
        data: { videoId: video.id, userId: liker.id },
      });
      likeCount += 1;
      if (liker.id !== video.userId) {
        await prisma.notification.create({
          data: {
            userId: video.userId,
            actorUserId: liker.id,
            type: "LIKE",
            title: "New like",
            body: `${liker.displayName} liked your video`,
            videoId: video.id,
            isRead: i % 2 === 0,
          },
        });
      }
    }

    const commenters = users.filter((u) => u.id !== video.userId).slice(0, 2);
    for (let c = 0; c < commenters.length; c += 1) {
      const commenter = commenters[c]!;
      const text = COMMENT_POOL[(i + c) % COMMENT_POOL.length]!;
      await prisma.videoComment.create({
        data: { videoId: video.id, userId: commenter.id, text },
      });
      commentCount += 1;
      await prisma.notification.create({
        data: {
          userId: video.userId,
          actorUserId: commenter.id,
          type: "COMMENT",
          title: "New comment",
          body: `${commenter.displayName}: ${text}`,
          videoId: video.id,
          isRead: false,
        },
      });
    }

    // A couple of saves
    if (i % 2 === 0) {
      await prisma.videoSave.create({
        data: { videoId: video.id, userId: alice.id },
      });
    }

    await prisma.video.update({
      where: { id: video.id },
      data: {
        likeCount: likers.length,
        commentCount: commenters.length,
      },
    });
  }

  // Follow notifications for alice
  for (const follower of [bob, carol, dave]) {
    await prisma.notification.create({
      data: {
        userId: alice.id,
        actorUserId: follower.id,
        type: "FOLLOW",
        title: "New follower",
        body: `${follower.displayName} started following you`,
        isRead: false,
      },
    });
  }

  // Open report for admin moderation queue
  const reportVideo = videos[0]!;
  await prisma.report.create({
    data: {
      reporterId: erin.id,
      targetType: "VIDEO",
      targetId: reportVideo.id,
      reason: "Spam / misleading\n\nThis looks like recycled promo content with a clickbait caption.",
      status: "OPEN",
    },
  });
  await prisma.report.create({
    data: {
      reporterId: dave.id,
      targetType: "USER",
      targetId: bob.id,
      reason: "Impersonation\n\nThis account is pretending to be someone else (demo report).",
      status: "OPEN",
    },
  });

  const welcome = await prisma.announcement.create({
    data: {
      title: "Welcome to local demo",
      body: "This announcement was seeded for admin/inbox testing. No Alibaba Cloud keys required.",
      createdById: admin.id,
      publishedAt: new Date(),
      isActive: true,
    },
  });

  // Inbox reads notifications — fan out so seeded announcements show on phones.
  const activeUsers = await prisma.user.findMany({
    where: { status: "ACTIVE", deletedAt: null },
    select: { id: true },
  });
  if (activeUsers.length > 0) {
    await prisma.notification.createMany({
      data: activeUsers.map((user) => ({
        userId: user.id,
        type: "ANNOUNCEMENT",
        title: welcome.title,
        body: welcome.body,
        isRead: false,
        createdAt: welcome.publishedAt ?? welcome.createdAt,
      })),
    });
  }

  return { likeCount, commentCount, followCount: followPairs.length };
}

async function main(): Promise<void> {
  console.log("Seeding demo data (@demo.local)...");
  await wipeDemoUsers();

  const passwordHash = await hashPassword(DEMO_PASSWORD);
  const users = await createUsers(passwordHash);
  const creators = users.filter((u) => u.role === "USER");
  // Give admin a couple of videos too
  const videos = await createVideos([...creators, users[0]!]);
  const social = await createSocialGraph(users, videos);

  const counts = {
    users: await prisma.user.count({ where: { email: { endsWith: "@demo.local" } } }),
    videos: await prisma.video.count({
      where: { user: { email: { endsWith: "@demo.local" } } },
    }),
    likes: await prisma.videoLike.count(),
    comments: await prisma.videoComment.count(),
    follows: await prisma.follow.count(),
    notifications: await prisma.notification.count(),
    reports: await prisma.report.count(),
    announcements: await prisma.announcement.count(),
  };

  console.log("\nDemo seed complete.");
  console.log(JSON.stringify({ ...counts, social }, null, 2));
  console.log("\nLogin (password for all):", DEMO_PASSWORD);
  console.log("  Admin:  admin@demo.local  (username: demo_admin)");
  console.log("  Users:  alice@demo.local, bob@demo.local, carol@demo.local,");
  console.log("          dave@demo.local, erin@demo.local");
}

main()
  .catch((error) => {
    console.error(error);
    process.exitCode = 1;
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
