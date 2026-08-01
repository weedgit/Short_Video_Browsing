import type { NextFunction, Request, Response } from "express";
import { timingSafeEqual } from "node:crypto";
import { AppError } from "../middleware/errorHandler";
import { config } from "../config";

/**
 * Optional auth for Alibaba VOD HTTP callbacks.
 * Configure a shared secret in the VOD console callback URL query (?auth=...)
 * or header X-Vod-Callback-Auth, matching ALIBABA_VOD_CALLBACK_AUTH_KEY.
 */
export function verifyAlibabaVodWebhook(
  req: Request,
  _res: Response,
  next: NextFunction,
): void {
  const secret = config.alibaba.vodCallbackAuthKey;
  if (!secret) {
    next();
    return;
  }

  const provided =
    (typeof req.query.auth === "string" ? req.query.auth : undefined) ??
    req.header("x-vod-callback-auth") ??
    req.header("x-alibaba-vod-auth");

  if (!provided) {
    next(new AppError(401, "INVALID_WEBHOOK_SIGNATURE", "Callback auth is missing."));
    return;
  }

  const a = Buffer.from(provided);
  const b = Buffer.from(secret);
  const valid = a.length === b.length && timingSafeEqual(a, b);
  if (!valid) {
    next(new AppError(401, "INVALID_WEBHOOK_SIGNATURE", "Callback auth is invalid."));
    return;
  }

  next();
}

/** @deprecated Use verifyAlibabaVodWebhook */
export const verifyCloudflareWebhook = verifyAlibabaVodWebhook;
