import type { NextFunction, Request, Response } from "express";
import { createHash, timingSafeEqual } from "node:crypto";
import { AppError } from "../middleware/errorHandler";
import { config } from "../config";

export function verifyCloudflareWebhook(
  req: Request,
  _res: Response,
  next: NextFunction,
): void {
  const secret = config.cloudflareWebhookSecret;
  if (!secret) {
    if (config.isProduction) {
      next(new AppError(503, "WEBHOOK_NOT_CONFIGURED", "Webhook secret is not configured."));
      return;
    }
    next();
    return;
  }

  const signature = req.header("webhook-signature") ?? req.header("x-webhook-signature");
  if (!signature || !req.rawBody) {
    next(new AppError(401, "INVALID_WEBHOOK_SIGNATURE", "Webhook signature is invalid."));
    return;
  }

  const expected = createHash("sha256").update(`${secret}${req.rawBody}`).digest("hex");
  const provided = signature.replace(/^sha256=/, "");

  const valid =
    expected.length === provided.length &&
    timingSafeEqual(Buffer.from(expected), Buffer.from(provided));

  if (!valid) {
    next(new AppError(401, "INVALID_WEBHOOK_SIGNATURE", "Webhook signature is invalid."));
    return;
  }

  next();
}
