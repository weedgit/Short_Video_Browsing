import type { Request, Response } from "express";
import { AppError } from "../middleware/errorHandler";
import { asyncHandler } from "../utils/asyncHandler";
import {
  createAnnouncement,
  deleteAnnouncement,
  getAnalytics,
  listAnnouncements,
  listReports,
  listUsers,
  listVideos,
  updateAnnouncement,
  updateReport,
  updateUser,
  updateVideoStatus,
} from "../service/admin.service";
import {
  adminCreateAnnouncementSchema,
  adminReportsQuerySchema,
  adminUpdateAnnouncementSchema,
  adminUpdateReportSchema,
  adminUpdateUserSchema,
  adminUpdateVideoSchema,
  adminUsersQuerySchema,
  adminVideosQuerySchema,
} from "../validators/admin.schema";
import type { z } from "zod";

function parseBody<S extends z.ZodTypeAny>(schema: S, body: unknown): z.output<S> {
  const parsed = schema.safeParse(body);
  if (!parsed.success) {
    const message = parsed.error.issues.map((issue) => issue.message).join(", ");
    throw new AppError(400, "VALIDATION_ERROR", message);
  }
  return parsed.data;
}

function parseQuery<S extends z.ZodTypeAny>(schema: S, query: unknown): z.output<S> {
  const parsed = schema.safeParse(query);
  if (!parsed.success) {
    const message = parsed.error.issues.map((issue) => issue.message).join(", ");
    throw new AppError(400, "VALIDATION_ERROR", message);
  }
  return parsed.data;
}

function sendData(res: Response, data: unknown, statusCode = 200): void {
  res.status(statusCode).json({
    data,
    request_id: res.getHeader("X-Request-Id"),
  });
}

function requireParam(value: string | undefined, name: string): string {
  if (!value) {
    throw new AppError(400, "VALIDATION_ERROR", `${name} is required`);
  }
  return value;
}

export const getAdminUsersHandler = asyncHandler(async (req: Request, res: Response) => {
  const query = parseQuery(adminUsersQuerySchema, req.query);
  const page = await listUsers(query);
  sendData(res, page);
});

export const patchAdminUserHandler = asyncHandler(async (req: Request, res: Response) => {
  const userId = requireParam(req.params.userId, "userId");
  const body = parseBody(adminUpdateUserSchema, req.body);
  const user = await updateUser(userId, body);
  sendData(res, user);
});

export const getAdminVideosHandler = asyncHandler(async (req: Request, res: Response) => {
  const query = parseQuery(adminVideosQuerySchema, req.query);
  const page = await listVideos(query);
  sendData(res, page);
});

export const patchAdminVideoHandler = asyncHandler(async (req: Request, res: Response) => {
  const videoId = requireParam(req.params.videoId, "videoId");
  const body = parseBody(adminUpdateVideoSchema, req.body);
  const video = await updateVideoStatus(videoId, body);
  sendData(res, video);
});

export const getAdminReportsHandler = asyncHandler(async (req: Request, res: Response) => {
  const query = parseQuery(adminReportsQuerySchema, req.query);
  const page = await listReports(query);
  sendData(res, page);
});

export const patchAdminReportHandler = asyncHandler(async (req: Request, res: Response) => {
  const id = requireParam(req.params.id, "id");
  const body = parseBody(adminUpdateReportSchema, req.body);
  const report = await updateReport(id, body);
  sendData(res, report);
});

export const getAdminAnnouncementsHandler = asyncHandler(async (req: Request, res: Response) => {
  const items = await listAnnouncements();
  sendData(res, { items });
});

export const postAdminAnnouncementHandler = asyncHandler(async (req: Request, res: Response) => {
  if (!req.userId) {
    throw new AppError(401, "UNAUTHORIZED", "Authentication is required.");
  }

  const body = parseBody(adminCreateAnnouncementSchema, req.body);
  const announcement = await createAnnouncement(req.userId, body);
  sendData(res, announcement, 201);
});

export const patchAdminAnnouncementHandler = asyncHandler(async (req: Request, res: Response) => {
  const id = requireParam(req.params.id, "id");
  const body = parseBody(adminUpdateAnnouncementSchema, req.body);
  const announcement = await updateAnnouncement(id, body);
  sendData(res, announcement);
});

export const deleteAdminAnnouncementHandler = asyncHandler(async (req: Request, res: Response) => {
  const id = requireParam(req.params.id, "id");
  await deleteAnnouncement(id);
  sendData(res, { success: true });
});

export const getAdminAnalyticsHandler = asyncHandler(async (_req: Request, res: Response) => {
  const analytics = await getAnalytics();
  sendData(res, analytics);
});
