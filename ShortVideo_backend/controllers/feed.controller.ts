import type { Request, Response } from "express";
import { AppError } from "../middleware/errorHandler";
import { asyncHandler } from "../utils/asyncHandler";
import { getClientDeviceInfo } from "../utils/device";
import { getFeedPage, ingestPlaybackEvents } from "../service/feed.service";
import { feedQuerySchema, playbackBatchSchema } from "../validators/feed.schema";
import type { z } from "zod";

function parseQuery<S extends z.ZodTypeAny>(schema: S, query: unknown): z.output<S> {
  const parsed = schema.safeParse(query);
  if (!parsed.success) {
    const message = parsed.error.issues.map((issue) => issue.message).join(", ");
    throw new AppError(400, "VALIDATION_ERROR", message);
  }
  return parsed.data;
}

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

function resolveAudience(req: Request) {
  const device = getClientDeviceInfo(req);
  return {
    userId: req.userId,
    deviceId: device.deviceId,
  };
}

export const getFeedHandler = asyncHandler(async (req: Request, res: Response) => {
  const query = parseQuery(feedQuerySchema, req.query);
  const page = await getFeedPage(query, resolveAudience(req));
  sendData(res, page);
});

export const postPlaybackEventsBatchHandler = asyncHandler(async (req: Request, res: Response) => {
  const body = parseBody(playbackBatchSchema, req.body);
  const result = await ingestPlaybackEvents(body, resolveAudience(req));
  sendData(res, result, 202);
});
