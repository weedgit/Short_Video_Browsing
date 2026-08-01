import { Router } from "express";
import { postAlibabaVodWebhookHandler } from "../controllers/webhook.controller";
import { verifyAlibabaVodWebhook } from "../middleware/verifyCloudflareWebhook";

export function createWebhookRouter(): Router {
  const router = Router();

  // Primary: Alibaba Cloud ApsaraVideo VOD HTTP callbacks
  router.post("/alibaba/vod", verifyAlibabaVodWebhook, postAlibabaVodWebhookHandler);

  // Backward-compatible alias (do not configure new Cloudflare hooks)
  router.post("/cloudflare/stream", verifyAlibabaVodWebhook, postAlibabaVodWebhookHandler);

  return router;
}
