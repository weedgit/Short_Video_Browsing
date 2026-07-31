import { AppError } from "../../middleware/errorHandler";
import {
  countActiveUploadSessions,
  createUploadSession,
  findUploadSessionById,
  findUploadSessionByTokenHash,
  findUploadSessionByVideoId,
  markUploadUrlUsed,
  publishVideoRecord,
  updateUploadProgress,
  cancelUploadSession,
} from "../../db/repositories/upload.repository";
import { buildAssetPlaybackUrls, createDirectUpload } from "../../integrations/cloudflare";
import type { CreateUploadResult, PublishVideoResult, UploadProgressResult } from "../../models/upload.types";
import { generateUploadToken, hashUploadToken } from "../../utils/uploadToken";
import type { CreateUploadInput, PublishVideoInput, UploadProgressInput } from "../../validators/upload.schema";
import {
  assertSingleActiveUpload,
  assertUploadTokenValid,
  validateUploadRequest,
} from "./upload-validation.service";
import { enqueueUploadProcessingJob } from "../../jobs/queues";

export async function createUpload(
  userId: string,
  input: CreateUploadInput,
): Promise<CreateUploadResult> {
  validateUploadRequest(input);

  const activeCount = await countActiveUploadSessions(userId);
  assertSingleActiveUpload(activeCount);

  const uploadToken = generateUploadToken();
  const uploadTokenHash = hashUploadToken(uploadToken);
  const maxDurationSeconds = Math.ceil((input.durationMs ?? 60_000) / 1000);

  const directUpload = await createDirectUpload({
    maxDurationSeconds,
    fileSizeBytes: input.fileSizeBytes,
  });

  const { uploadId, videoId } = await createUploadSession({
    userId,
    mimeType: input.mimeType,
    fileSizeBytes: BigInt(input.fileSizeBytes),
    durationMs: input.durationMs,
    uploadUrl: directUpload.uploadUrl,
    uploadTokenHash,
    uploadUrlExpiresAt: directUpload.expiresAt,
    cloudflareAssetId: directUpload.cloudflareAssetId,
  });

  return {
    uploadId,
    videoId,
    uploadUrl: directUpload.uploadUrl,
    uploadToken,
    uploadUrlExpiresAt: directUpload.expiresAt.toISOString(),
    status: "DRAFT",
  };
}

export async function reportUploadProgress(
  userId: string,
  uploadId: string,
  uploadToken: string,
  input: UploadProgressInput,
): Promise<UploadProgressResult> {
  const session = await findUploadSessionById(uploadId);
  if (!session || session.userId !== userId) {
    throw new AppError(404, "UPLOAD_NOT_FOUND", "Upload session not found.");
  }

  if (hashUploadToken(uploadToken) !== session.uploadTokenHash) {
    throw new AppError(403, "INVALID_UPLOAD_TOKEN", "Invalid upload token.");
  }

  assertUploadTokenValid({
    expiresAt: session.uploadUrlExpiresAt,
  });

  if (!session.uploadUrlUsedAt) {
    await markUploadUrlUsed(uploadId);
  }

  const nextStatus = input.bytesUploaded >= Number(session.fileSizeBytes) ? "UPLOADED" : "UPLOADING";
  await updateUploadProgress(uploadId, BigInt(input.bytesUploaded), nextStatus);

  return {
    uploadId,
    videoId: session.videoId,
    status: nextStatus,
    bytesUploaded: String(input.bytesUploaded),
    fileSizeBytes: session.fileSizeBytes.toString(),
  };
}

export async function publishVideo(
  userId: string,
  videoId: string,
  input: PublishVideoInput,
): Promise<PublishVideoResult> {
  const session = await findUploadSessionByVideoId(videoId, userId);
  if (!session) {
    throw new AppError(404, "VIDEO_NOT_FOUND", "Video to publish was not found.");
  }

  if (!["UPLOADED", "PROCESSING", "PUBLISHED"].includes(session.status)) {
    throw new AppError(409, "UPLOAD_NOT_READY", "Upload is not complete yet.");
  }

  await publishVideoRecord({
    videoId,
    userId,
    description: input.description,
    category: input.category,
    hashtags: input.hashtags,
  });

  return {
    videoId,
    status: "PUBLISHED",
  };
}

export async function simulateDevUploadComplete(
  userId: string,
  uploadId: string,
  uploadToken: string,
): Promise<void> {
  if (process.env.NODE_ENV === "production") {
    throw new AppError(404, "NOT_FOUND", "Not found");
  }

  const session = await findUploadSessionById(uploadId);
  if (!session || session.userId !== userId) {
    throw new AppError(404, "UPLOAD_NOT_FOUND", "Upload session not found.");
  }

  if (hashUploadToken(uploadToken) !== session.uploadTokenHash) {
    throw new AppError(403, "INVALID_UPLOAD_TOKEN", "Invalid upload token.");
  }

  assertUploadTokenValid({
    expiresAt: session.uploadUrlExpiresAt,
  });

  if (!session.uploadUrlUsedAt) {
    await markUploadUrlUsed(uploadId);
  }

  const assetId = session.cloudflareAssetId ?? `dev-${uploadId}`;
  const urls = buildAssetPlaybackUrls(assetId);

  await updateUploadProgress(uploadId, session.fileSizeBytes, "UPLOADED");
  await enqueueUploadProcessingJob({
    videoId: session.videoId,
    uploadId,
    cloudflareAssetId: assetId,
    hlsUrl: urls.hlsUrl,
    streamUrl: urls.streamUrl,
    durationMs: session.durationMs ?? undefined,
  });
}

export async function cancelUpload(userId: string, uploadId: string): Promise<void> {
  const session = await findUploadSessionById(uploadId);
  if (!session || session.userId !== userId) {
    throw new AppError(404, "UPLOAD_NOT_FOUND", "Upload session not found.");
  }

  if (!["DRAFT", "UPLOADING", "UPLOADED", "PROCESSING"].includes(session.status)) {
    throw new AppError(409, "UPLOAD_NOT_CANCELLABLE", "This upload cannot be cancelled.");
  }

  await cancelUploadSession(uploadId, userId);
}
