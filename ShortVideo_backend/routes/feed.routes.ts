import { Router } from "express";
import { getFeedHandler } from "../controllers/feed.controller";
import { postPlaybackEventsBatchHandler } from "../controllers/feed.controller";
import { optionalAuthenticate } from "../middleware/optionalAuthenticate";

export function createFeedRouter(): Router {
  const router = Router();

  router.get("/", optionalAuthenticate, getFeedHandler);

  return router;
}

export function createPlaybackRouter(): Router {
  const router = Router();

  router.post("/events/batch", optionalAuthenticate, postPlaybackEventsBatchHandler);

  return router;
}
