import { Router } from "express";
import { authenticate } from "../middleware/authenticate";
import { optionalAuthenticate } from "../middleware/optionalAuthenticate";
import {
  deleteFollowUserHandler,
  deleteLikeVideoHandler,
  deleteSaveVideoHandler,
  getDiscoverHandler,
  getInboxHandler,
  getMyProfileHandler,
  getUserProfileHandler,
  getUserVideosHandler,
  getVideoCommentsHandler,
  postDeviceFcmHandler,
  postFollowUserHandler,
  postInboxItemReadHandler,
  postInboxReadAllHandler,
  postLikeVideoHandler,
  postReportHandler,
  postSaveVideoHandler,
  postVideoCommentHandler,
} from "../controllers/social.controller";

export function createSocialVideoRouter(): Router {
  const router = Router();

  router.post("/:videoId/like", authenticate, postLikeVideoHandler);
  router.delete("/:videoId/like", authenticate, deleteLikeVideoHandler);
  router.get("/:videoId/comments", optionalAuthenticate, getVideoCommentsHandler);
  router.post("/:videoId/comments", authenticate, postVideoCommentHandler);
  router.post("/:videoId/save", authenticate, postSaveVideoHandler);
  router.delete("/:videoId/save", authenticate, deleteSaveVideoHandler);

  return router;
}

export function createSocialUserRouter(): Router {
  const router = Router();

  // NOTE: literal routes must be registered before the ":userId" wildcard routes.
  router.get("/me/profile", authenticate, getMyProfileHandler);
  router.post("/:userId/follow", authenticate, postFollowUserHandler);
  router.delete("/:userId/follow", authenticate, deleteFollowUserHandler);
  router.get("/:userId/videos", optionalAuthenticate, getUserVideosHandler);
  router.get("/:userId/profile", optionalAuthenticate, getUserProfileHandler);

  return router;
}

export function createDiscoverRouter(): Router {
  const router = Router();

  router.get("/", optionalAuthenticate, getDiscoverHandler);

  return router;
}

export function createInboxRouter(): Router {
  const router = Router();

  router.get("/", getInboxHandler);
  router.post("/read-all", postInboxReadAllHandler);
  router.post("/:id/read", postInboxItemReadHandler);

  return router;
}

export function createDeviceRouter(): Router {
  const router = Router();

  router.post("/fcm", postDeviceFcmHandler);

  return router;
}

export function createReportsRouter(): Router {
  const router = Router();

  router.post("/", postReportHandler);

  return router;
}
