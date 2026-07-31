import type { Request, Response } from "express";
import { AppError } from "../middleware/errorHandler";
import { asyncHandler } from "../utils/asyncHandler";
import {
  createUpload,
  publishVideo,
  reportUploadProgress,
  simulateDevUploadComplete,
  cancelUpload,
} from "../service/upload/upload.service";
import {
  createUploadSchema,
  publishVideoSchema,
  uploadProgressSchema,
} from "../validators/upload.schema";
import type { z } from "zod";

function parseBody<S extends z.ZodTypeAny>(schema: S, body: unknown): z.output<S> {
  const parsed = schema.safeParse(body);
  if (!parsed.success) {
    const message = parsed.error.issues.map((issue) => issue.message).join(", ");
    throw new AppError(400, "VALIDATION_ERROR", message);
  }
  return parsed.data;
}

function sendData(res: Response, data: unknown, statusCode = 200): void {
  res.status(statusCode).json({
    data,
    request_id: res.getHeader("X-Request-Id"),
  });
}

function requireUserId(req: Request): string {
  if (!req.userId) {
    throw new AppError(401, "UNAUTHORIZED", "Authentication is required.");
  }
  return req.userId;
}

export const postCreateUploadHandler = asyncHandler(async (req: Request, res: Response) => {
  const body = parseBody(createUploadSchema, req.body);
  const result = await createUpload(requireUserId(req), body);
  sendData(res, result, 201);
});

function requireRouteParam(value: string | undefined, name: string): string {
  if (!value) {
    throw new AppError(400, "VALIDATION_ERROR", `${name} is required`);
  }
  return value;
}

export const patchUploadProgressHandler = asyncHandler(async (req: Request, res: Response) => {
  const body = parseBody(uploadProgressSchema, req.body);
  const uploadToken = req.header("x-upload-token")?.trim();
  if (!uploadToken) {
    throw new AppError(401, "UPLOAD_TOKEN_REQUIRED", "The X-Upload-Token header is required.");
  }

  const uploadId = requireRouteParam(req.params.uploadId, "uploadId");
  const result = await reportUploadProgress(requireUserId(req), uploadId, uploadToken, body);
  sendData(res, result);
});

export const postDevUploadCompleteHandler = asyncHandler(async (req: Request, res: Response) => {
  const uploadToken = req.header("x-upload-token")?.trim();
  if (!uploadToken) {
    throw new AppError(401, "UPLOAD_TOKEN_REQUIRED", "The X-Upload-Token header is required.");
  }

  const uploadId = requireRouteParam(req.params.uploadId, "uploadId");
  await simulateDevUploadComplete(requireUserId(req), uploadId, uploadToken);
  sendData(res, { success: true }, 202);
});

export const postPublishVideoHandler = asyncHandler(async (req: Request, res: Response) => {
  const body = parseBody(publishVideoSchema, req.body);
  const videoId = requireRouteParam(req.params.videoId, "videoId");
  const result = await publishVideo(requireUserId(req), videoId, body);
  sendData(res, result);
});

export const deleteUploadHandler = asyncHandler(async (req: Request, res: Response) => {
  const uploadId = requireRouteParam(req.params.uploadId, "uploadId");
  await cancelUpload(requireUserId(req), uploadId);
  sendData(res, { uploadId, cancelled: true });
});
