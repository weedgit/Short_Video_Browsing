import { Router } from "express";
import {
  deleteAdminAnnouncementHandler,
  getAdminAnalyticsHandler,
  getAdminAnnouncementsHandler,
  getAdminReportsHandler,
  getAdminUserHandler,
  getAdminUsersHandler,
  getAdminVideoHandler,
  getAdminVideosHandler,
  patchAdminAnnouncementHandler,
  patchAdminReportHandler,
  patchAdminUserHandler,
  patchAdminVideoHandler,
  postAdminAnnouncementHandler,
} from "../controllers/admin.controller";

export function createAdminRouter(): Router {
  const router = Router();

  router.get("/users", getAdminUsersHandler);
  router.get("/users/:userId", getAdminUserHandler);
  router.patch("/users/:userId", patchAdminUserHandler);
  router.get("/videos", getAdminVideosHandler);
  router.get("/videos/:videoId", getAdminVideoHandler);
  router.patch("/videos/:videoId", patchAdminVideoHandler);
  router.get("/reports", getAdminReportsHandler);
  router.patch("/reports/:id", patchAdminReportHandler);
  router.get("/announcements", getAdminAnnouncementsHandler);
  router.post("/announcements", postAdminAnnouncementHandler);
  router.patch("/announcements/:id", patchAdminAnnouncementHandler);
  router.delete("/announcements/:id", deleteAdminAnnouncementHandler);
  router.get("/analytics", getAdminAnalyticsHandler);

  return router;
}
