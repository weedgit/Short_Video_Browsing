import type { Request, Response } from "express";
import { asyncHandler } from "../utils/asyncHandler";
import { handleAlibabaVodWebhook } from "../service/upload/upload-webhook.service";

function sendData(res: Response, data: unknown, statusCode = 200): void {
  res.status(statusCode).json({
    data,
    request_id: res.getHeader("X-Request-Id"),
  });
}

export const postAlibabaVodWebhookHandler = asyncHandler(async (req: Request, res: Response) => {
  const eventId =
    req.header("x-vod-request-id") ??
    req.header("x-acs-request-id") ??
    req.header("x-webhook-id") ??
    `${Date.now()}-${JSON.stringify(req.body).slice(0, 48)}`;

  const result = await handleAlibabaVodWebhook({
    eventId,
    payload: req.body,
    rawBody: req.rawBody?.toString("utf8") ?? JSON.stringify(req.body),
  });

  sendData(res, result, result.duplicate ? 200 : 202);
});

/** @deprecated */
export const postCloudflareStreamWebhookHandler = postAlibabaVodWebhookHandler;
