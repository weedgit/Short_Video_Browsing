import { Router } from "express";
import { postPublishVideoHandler } from "../controllers/upload.controller";

export function createVideoRouter(): Router {
  const router = Router();

  router.post("/:videoId/publish", postPublishVideoHandler);

  return router;
}
