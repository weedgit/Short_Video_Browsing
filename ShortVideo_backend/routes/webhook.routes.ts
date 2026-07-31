import { Router } from "express";
import { postCloudflareStreamWebhookHandler } from "../controllers/webhook.controller";
import { verifyCloudflareWebhook } from "../middleware/verifyCloudflareWebhook";

export function createWebhookRouter(): Router {
  const router = Router();

  router.post("/cloudflare/stream", verifyCloudflareWebhook, postCloudflareStreamWebhookHandler);

  return router;
}
