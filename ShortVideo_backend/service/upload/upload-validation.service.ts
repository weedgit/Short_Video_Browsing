import { AppError } from "../../middleware/errorHandler";
import { config } from "../../config";
import type { CreateUploadInput } from "../../validators/upload.schema";

const allowedMimeTypes = new Set(["video/mp4", "video/quicktime", "video/webm"]);

export function validateUploadRequest(input: CreateUploadInput): void {
  if (!allowedMimeTypes.has(input.mimeType)) {
    throw new AppError(400, "UNSUPPORTED_MIME_TYPE", "Unsupported video format.");
  }

  if (input.fileSizeBytes > config.upload.maxFileSizeBytes) {
    throw new AppError(400, "FILE_TOO_LARGE", "File size exceeds the allowed limit.");
  }

  if (input.durationMs != null && input.durationMs > config.upload.maxDurationMs) {
    throw new AppError(400, "VIDEO_TOO_LONG", "Video duration exceeds the allowed limit.");
  }
}

export function assertSingleActiveUpload(activeCount: number): void {
  if (activeCount >= config.upload.maxConcurrentUploadsPerUser) {
    throw new AppError(409, "UPLOAD_IN_PROGRESS", "An upload is already in progress.");
  }
}

export function assertUploadTokenValid(params: {
  expiresAt: Date;
}): void {
  if (params.expiresAt.getTime() <= Date.now()) {
    throw new AppError(410, "UPLOAD_URL_EXPIRED", "Upload URL has expired.");
  }
}

export function assertUploadUrlNotReused(usedAt: Date | null): void {
  if (usedAt) {
    throw new AppError(409, "UPLOAD_URL_USED", "Upload URL has already been used.");
  }
}
