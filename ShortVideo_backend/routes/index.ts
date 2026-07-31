import { Router } from "express";
import { healthRouter, apiRouter } from "./health.routes";
import { createAuthRouter } from "./auth.routes";
import { createFeedRouter, createPlaybackRouter } from "./feed.routes";
import { createUploadRouter } from "./upload.routes";
import { createVideoRouter } from "./video.routes";
import { createWebhookRouter } from "./webhook.routes";
import { deleteAccountHandler } from "../controllers/auth.controller";
import { authenticate } from "../middleware/authenticate";
import { createDocsRouter } from "./docs.routes";

export async function createRoutes(): Promise<Router> {
  const router = Router();

  router.use("/health", healthRouter);
  router.use(createDocsRouter());
  router.use("/v1", apiRouter);
  router.use("/v1/auth", await createAuthRouter());
  router.use("/v1/feed", createFeedRouter());
  router.use("/v1/playback", createPlaybackRouter());
  router.use("/v1/uploads", authenticate, createUploadRouter());
  router.use("/v1/videos", authenticate, createVideoRouter());
  router.use("/v1/webhooks", createWebhookRouter());
  router.delete("/v1/account", authenticate, deleteAccountHandler);

  return router;
}
