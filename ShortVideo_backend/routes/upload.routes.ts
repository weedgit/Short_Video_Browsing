import { Router } from "express";
import {
  patchUploadProgressHandler,
  postCreateUploadHandler,
  postDevUploadCompleteHandler,
  deleteUploadHandler,
} from "../controllers/upload.controller";

export function createUploadRouter(): Router {
  const router = Router();

  router.post("/", postCreateUploadHandler);
  router.patch("/:uploadId/progress", patchUploadProgressHandler);
  router.delete("/:uploadId", deleteUploadHandler);

  if (process.env.NODE_ENV !== "production") {
    router.post("/:uploadId/dev-complete", postDevUploadCompleteHandler);
  }

  return router;
}
