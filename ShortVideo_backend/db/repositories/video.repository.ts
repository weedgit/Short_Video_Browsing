import type { Prisma, Video } from "@prisma/client";
import { getPrismaClient } from "../client";
import { hashPassword } from "../../utils/password";

export type VideoWithRelations = Prisma.VideoGetPayload<{
  include: {
    user: true;
    hashtags: true;
  };
}>;

export async function findFeedVideos(params: {
  limit: number;
  cursor?: { createdAt: Date; id: string };
  excludeVideoIds: string[];
  followingUserIds?: string[];
}): Promise<VideoWithRelations[]> {
  const prisma = getPrismaClient();

  return prisma.video.findMany({
    where: {
      status: "READY",
      deletedAt: null,
      id: {
        notIn: params.excludeVideoIds.length > 0 ? params.excludeVideoIds : undefined,
      },
      ...(params.followingUserIds ? { userId: { in: params.followingUserIds } } : {}),
      ...(params.cursor
        ? {
            OR: [
              { createdAt: { lt: params.cursor.createdAt } },
              {
                createdAt: params.cursor.createdAt,
                id: { lt: params.cursor.id },
              },
            ],
          }
        : {}),
    },
    include: {
      user: true,
      hashtags: true,
    },
    orderBy: [{ createdAt: "desc" }, { id: "desc" }],
    take: params.limit,
  });
}

export async function countReadyVideos(followingUserIds?: string[]): Promise<number> {
  const prisma = getPrismaClient();
  return prisma.video.count({
    where: {
      status: "READY",
      deletedAt: null,
      ...(followingUserIds ? { userId: { in: followingUserIds } } : {}),
    },
  });
}

export async function findVideoIdsByIds(videoIds: string[]): Promise<string[]> {
  if (videoIds.length === 0) return [];

  const prisma = getPrismaClient();
  const rows = await prisma.video.findMany({
    where: {
      id: { in: videoIds },
      status: "READY",
      deletedAt: null,
    },
    select: { id: true },
  });

  return rows.map((row) => row.id);
}

export async function createFeedImpressions(params: {
  videoIds: string[];
  userId?: string;
  deviceId?: string;
}): Promise<void> {
  if (params.videoIds.length === 0) return;

  const prisma = getPrismaClient();
  await prisma.feedImpression.createMany({
    data: params.videoIds.map((videoId) => ({
      videoId,
      userId: params.userId,
      deviceId: params.deviceId,
    })),
    skipDuplicates: true,
  });
}

export async function findImpressedVideoIds(params: {
  userId?: string;
  deviceId?: string;
}): Promise<string[]> {
  const prisma = getPrismaClient();

  if (params.userId) {
    const rows = await prisma.feedImpression.findMany({
      where: { userId: params.userId },
      select: { videoId: true },
    });
    return rows.map((row) => row.videoId);
  }

  if (params.deviceId) {
    const rows = await prisma.feedImpression.findMany({
      where: { deviceId: params.deviceId },
      select: { videoId: true },
    });
    return rows.map((row) => row.videoId);
  }

  return [];
}

export async function clearFeedImpressions(params: {
  userId?: string;
  deviceId?: string;
}): Promise<void> {
  const prisma = getPrismaClient();

  if (params.userId) {
    await prisma.feedImpression.deleteMany({ where: { userId: params.userId } });
    return;
  }

  if (params.deviceId) {
    await prisma.feedImpression.deleteMany({ where: { deviceId: params.deviceId } });
  }
}

export async function createPlaybackEvents(
  events: Array<{
    videoId: string;
    userId?: string;
    deviceId?: string;
    eventType: string;
    positionMs: number;
    occurredAt: Date;
  }>,
): Promise<number> {
  if (events.length === 0) return 0;

  const prisma = getPrismaClient();
  const result = await prisma.playbackEvent.createMany({
    data: events,
  });

  return result.count;
}

export async function ensureDevFeedSeed(): Promise<number> {
  const prisma = getPrismaClient();
  const existing = await prisma.video.count();
  if (existing > 0) return existing;

  const owner = await findOrCreateDevFeedOwner();
  return seedDevFeedVideosIfEmpty(owner.id);
}

async function findOrCreateDevFeedOwner() {
  const prisma = getPrismaClient();
  const existingOwner = await prisma.user.findFirst({
    where: { status: "ACTIVE", deletedAt: null },
    orderBy: { createdAt: "asc" },
  });

  if (existingOwner) {
    return existingOwner;
  }

  return prisma.user.create({
    data: {
      email: "feed-seed@shortvideo.local",
      username: "feed_seed",
      displayName: "SampleLib",
      passwordHash: await hashPassword("feed-seed-not-for-login"),
    },
  });
}

export async function seedDevFeedVideosIfEmpty(ownerUserId: string): Promise<number> {
  const prisma = getPrismaClient();
  const existing = await prisma.video.count();
  if (existing > 0) return existing;

  const samples = buildDevFeedSamples(ownerUserId);
  for (const sample of samples) {
    await prisma.video.create({
      data: sample,
    });
  }

  return samples.length;
}

function buildDevFeedSamples(ownerUserId: string) {
  const baseSamples = [
    {
      description: "Network sample clip (5 seconds)",
      streamUrl: "https://download.samplelib.com/mp4/sample-5s.mp4",
      durationMs: 5_000,
      hashtags: ["#network", "#sample"],
      category: "Demo",
    },
    {
      description: "Network sample clip (10 seconds)",
      streamUrl: "https://download.samplelib.com/mp4/sample-10s.mp4",
      durationMs: 10_000,
      hashtags: ["#network", "#feed"],
      category: "Demo",
    },
    {
      description: "Network sample clip (15 seconds)",
      streamUrl: "https://download.samplelib.com/mp4/sample-15s.mp4",
      durationMs: 15_000,
      hashtags: ["#shorts", "#demo"],
      category: "Demo",
    },
    {
      description: "Network sample clip (20 seconds)",
      streamUrl: "https://download.samplelib.com/mp4/sample-20s.mp4",
      durationMs: 20_000,
      hashtags: ["#feed", "#sample"],
      category: "Demo",
    },
    {
      description: "Network sample clip (30 seconds)",
      streamUrl: "https://download.samplelib.com/mp4/sample-30s.mp4",
      durationMs: 30_000,
      hashtags: ["#longform", "#sample"],
      category: "Demo",
    },
  ] as const;

  const samples: Prisma.VideoCreateInput[] = [];

  for (let index = 0; index < 24; index += 1) {
    const template = baseSamples[index % baseSamples.length]!;
    samples.push({
      user: { connect: { id: ownerUserId } },
      description: `${template.description} #${index + 1}`,
      durationMs: template.durationMs,
      status: "READY",
      streamUrl: template.streamUrl,
      category: template.category,
      hashtags: {
        create: template.hashtags.map((tag) => ({ tag })),
      },
    });
  }

  return samples;
}

export type { Video };
