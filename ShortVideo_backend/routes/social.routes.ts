import { Router } from "express";
import multer from "multer";
import path from "node:path";
import { mkdirSync } from "node:fs";
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
  patchMyProfileHandler,
  postDeviceFcmHandler,
  postFollowUserHandler,
  postInboxItemReadHandler,
  postInboxReadAllHandler,
  postLikeVideoHandler,
  postMyAvatarHandler,
  postReportHandler,
  postSaveVideoHandler,
  postVideoCommentHandler,
} from "../controllers/social.controller";

const avatarDir = path.resolve(process.cwd(), "storage", "avatars");
mkdirSync(avatarDir, { recursive: true });

const avatarUpload = multer({
  storage: multer.diskStorage({
    destination: (_req, _file, cb) => cb(null, avatarDir),
    filename: (req, file, cb) => {
      const userId = req.userId ?? "anon";
      const ext = path.extname(file.originalname || "").toLowerCase() || ".jpg";
      const safeExt = [".jpg", ".jpeg", ".png", ".webp", ".gif"].includes(ext) ? ext : ".jpg";
      cb(null, `${userId}-${Date.now()}${safeExt}`);
    },
  }),
  limits: { fileSize: 5 * 1024 * 1024 },
  fileFilter: (_req, file, cb) => {
    if (!file.mimetype.startsWith("image/")) {
      cb(new Error("Only image files are allowed for avatars."));
      return;
    }
    cb(null, true);
  },
});

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
  router.patch("/me/profile", authenticate, patchMyProfileHandler);
  router.post(
    "/me/avatar",
    authenticate,
    avatarUpload.single("avatar"),
    postMyAvatarHandler,
  );
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
