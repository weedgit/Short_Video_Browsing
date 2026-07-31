import type { Request, Response } from "express";
import { asyncHandler } from "../utils/asyncHandler";
import { handleCloudflareStreamWebhook } from "../service/upload/upload-webhook.service";

function sendData(res: Response, data: unknown, statusCode = 200): void {
  res.status(statusCode).json({
    data,
    request_id: res.getHeader("X-Request-Id"),
  });
}

export const postCloudflareStreamWebhookHandler = asyncHandler(async (req: Request, res: Response) => {
  const eventId =
    req.header("cf-webhook-id") ??
    req.header("x-webhook-id") ??
    `${Date.now()}-${JSON.stringify(req.body).slice(0, 32)}`;

  const result = await handleCloudflareStreamWebhook({
    eventId,
    payload: req.body,
    rawBody: req.rawBody?.toString("utf8") ?? JSON.stringify(req.body),
  });

  sendData(res, result, result.duplicate ? 200 : 202);
});
