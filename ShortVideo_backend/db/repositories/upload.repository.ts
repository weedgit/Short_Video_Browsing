import type { Prisma, UploadStatus } from "@prisma/client";
import { getPrismaClient } from "../client";

export async function countActiveUploadSessions(userId: string): Promise<number> {
  const prisma = getPrismaClient();
  return prisma.uploadSession.count({
    where: {
      userId,
      status: {
        in: ["DRAFT", "UPLOADING", "UPLOADED", "PROCESSING"],
      },
    },
  });
}

export async function createUploadSession(params: {
  userId: string;
  mimeType: string;
  fileSizeBytes: bigint;
  durationMs?: number;
  uploadUrl: string;
  uploadTokenHash: string;
  uploadUrlExpiresAt: Date;
  cloudflareAssetId?: string;
}): Promise<{ uploadId: string; videoId: string }> {
  const prisma = getPrismaClient();

  return prisma.$transaction(async (tx) => {
    const video = await tx.video.create({
      data: {
        userId: params.userId,
        status: "PROCESSING",
      },
    });

    const session = await tx.uploadSession.create({
      data: {
        userId: params.userId,
        videoId: video.id,
        mimeType: params.mimeType,
        fileSizeBytes: params.fileSizeBytes,
        durationMs: params.durationMs,
        uploadUrl: params.uploadUrl,
        uploadTokenHash: params.uploadTokenHash,
        uploadUrlExpiresAt: params.uploadUrlExpiresAt,
        cloudflareAssetId: params.cloudflareAssetId,
        status: "DRAFT",
      },
    });

    return { uploadId: session.id, videoId: video.id };
  });
}

export async function findUploadSessionById(uploadId: string) {
  const prisma = getPrismaClient();
  return prisma.uploadSession.findUnique({
    where: { id: uploadId },
    include: { video: true },
  });
}

export async function findUploadSessionByTokenHash(tokenHash: string) {
  const prisma = getPrismaClient();
  return prisma.uploadSession.findUnique({
    where: { uploadTokenHash: tokenHash },
    include: { video: true },
  });
}

export async function findUploadSessionByVideoId(videoId: string, userId: string) {
  const prisma = getPrismaClient();
  return prisma.uploadSession.findFirst({
    where: { videoId, userId },
    include: { video: true },
  });
}

export async function markUploadUrlUsed(uploadId: string): Promise<void> {
  const prisma = getPrismaClient();
  await prisma.uploadSession.update({
    where: { id: uploadId },
    data: {
      uploadUrlUsedAt: new Date(),
      status: "UPLOADING",
    },
  });
}

export async function updateUploadProgress(
  uploadId: string,
  bytesUploaded: bigint,
  status: UploadStatus,
): Promise<void> {
  const prisma = getPrismaClient();
  await prisma.uploadSession.update({
    where: { id: uploadId },
    data: {
      bytesUploaded,
      status,
    },
  });
}

export async function completeUploadSession(params: {
  uploadId: string;
  cloudflareAssetId: string;
  durationMs?: number;
}): Promise<void> {
  const prisma = getPrismaClient();
  const session = await prisma.uploadSession.findUniqueOrThrow({
    where: { id: params.uploadId },
  });

  await prisma.$transaction([
    prisma.uploadSession.update({
      where: { id: params.uploadId },
      data: {
        status: "PROCESSING",
        cloudflareAssetId: params.cloudflareAssetId,
      },
    }),
    prisma.video.update({
      where: { id: session.videoId },
      data: {
        cloudflareAssetId: params.cloudflareAssetId,
        durationMs: params.durationMs,
        status: "PROCESSING",
      },
    }),
  ]);
}

export async function publishVideoRecord(params: {
  videoId: string;
  userId: string;
  description: string;
  category?: string;
  hashtags: string[];
}): Promise<void> {
  const prisma = getPrismaClient();

  await prisma.$transaction(async (tx) => {
    await tx.videoHashtag.deleteMany({ where: { videoId: params.videoId } });
    await tx.video.update({
      where: { id: params.videoId, userId: params.userId },
      data: {
        description: params.description,
        category: params.category,
        status: "READY",
      },
    });

    if (params.hashtags.length > 0) {
      await tx.videoHashtag.createMany({
        data: params.hashtags.map((tag) => ({
          videoId: params.videoId,
          tag: tag.startsWith("#") ? tag : `#${tag}`,
        })),
      });
    }

    const session = await tx.uploadSession.findUnique({ where: { videoId: params.videoId } });
    if (session) {
      await tx.uploadSession.update({
        where: { id: session.id },
        data: { status: "PUBLISHED" },
      });
    }
  });
}

export async function finalizeProcessedVideo(params: {
  videoId: string;
  cloudflareAssetId: string;
  hlsUrl?: string;
  streamUrl?: string;
  durationMs?: number;
  thumbnailUrl?: string;
}): Promise<void> {
  const prisma = getPrismaClient();
  await prisma.video.update({
    where: { id: params.videoId },
    data: {
      cloudflareAssetId: params.cloudflareAssetId,
      hlsUrl: params.hlsUrl,
      streamUrl: params.streamUrl,
      durationMs: params.durationMs ?? undefined,
      thumbnailUrl: params.thumbnailUrl ?? undefined,
      status: params.hlsUrl || params.streamUrl ? "READY" : "PROCESSING",
    },
  });
}

export async function recordWebhookEvent(params: {
  provider: string;
  eventId: string;
  payloadHash: string;
}): Promise<boolean> {
  const prisma = getPrismaClient();
  try {
    await prisma.webhookEvent.create({
      data: params,
    });
    return true;
  } catch {
    return false;
  }
}

export async function resetUploadTestData(): Promise<void> {
  const prisma = getPrismaClient();
  await prisma.webhookEvent.deleteMany();
  await prisma.uploadSession.deleteMany();
}

export async function cancelUploadSession(uploadId: string, userId: string): Promise<void> {
  const prisma = getPrismaClient();
  const session = await prisma.uploadSession.findUnique({
    where: { id: uploadId },
  });

  if (!session || session.userId !== userId) {
    return;
  }

  if (!["DRAFT", "UPLOADING", "UPLOADED", "PROCESSING"].includes(session.status)) {
    return;
  }

  await prisma.$transaction([
    prisma.uploadSession.update({
      where: { id: uploadId },
      data: { status: "FAILED" },
    }),
    prisma.video.update({
      where: { id: session.videoId },
      data: { status: "DELETED" },
    }),
  ]);
}

export type { Prisma };
