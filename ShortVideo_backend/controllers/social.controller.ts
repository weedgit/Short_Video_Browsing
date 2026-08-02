import type { Request, Response } from "express";
import { AppError } from "../middleware/errorHandler";
import { asyncHandler } from "../utils/asyncHandler";
import {
  createVideoComment,
  discover,
  followUser,
  getInbox,
  getUserProfile,
  likeVideo,
  listComments,
  listLikedVideos,
  listSavedVideos,
  listUserVideos,
  markAllInboxRead,
  markInboxItemRead,
  registerFcmToken,
  saveVideo,
  submitReport,
  unfollowUser,
  unlikeVideo,
  unsaveVideo,
  updateMyAvatarUrl,
  updateMyProfile,
} from "../service/social.service";
import {
  commentCreateSchema,
  commentsQuerySchema,
  createReportSchema,
  discoverQuerySchema,
  inboxQuerySchema,
  registerFcmTokenSchema,
  updateProfileSchema,
  userVideosQuerySchema,
} from "../validators/social.schema";
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

function requireUserId(req: Request): string {
  if (!req.userId) {
    throw new AppError(401, "UNAUTHORIZED", "Authentication is required.");
  }
  return req.userId;
}

function requireParam(value: string | undefined, name: string): string {
  if (!value) {
    throw new AppError(400, "VALIDATION_ERROR", `${name} is required`);
  }
  return value;
}

export const postLikeVideoHandler = asyncHandler(async (req: Request, res: Response) => {
  const videoId = requireParam(req.params.videoId, "videoId");
  const result = await likeVideo(videoId, requireUserId(req));
  sendData(res, result);
});

export const deleteLikeVideoHandler = asyncHandler(async (req: Request, res: Response) => {
  const videoId = requireParam(req.params.videoId, "videoId");
  const result = await unlikeVideo(videoId, requireUserId(req));
  sendData(res, result);
});

export const getVideoCommentsHandler = asyncHandler(async (req: Request, res: Response) => {
  const videoId = requireParam(req.params.videoId, "videoId");
  const query = parseQuery(commentsQuerySchema, req.query);
  const page = await listComments(videoId, query);
  sendData(res, page);
});

export const postVideoCommentHandler = asyncHandler(async (req: Request, res: Response) => {
  const videoId = requireParam(req.params.videoId, "videoId");
  const body = parseBody(commentCreateSchema, req.body);
  const comment = await createVideoComment(videoId, requireUserId(req), body);
  sendData(res, comment, 201);
});

export const postFollowUserHandler = asyncHandler(async (req: Request, res: Response) => {
  const userId = requireParam(req.params.userId, "userId");
  const result = await followUser(requireUserId(req), userId);
  sendData(res, result);
});

export const deleteFollowUserHandler = asyncHandler(async (req: Request, res: Response) => {
  const userId = requireParam(req.params.userId, "userId");
  const result = await unfollowUser(requireUserId(req), userId);
  sendData(res, result);
});

export const postSaveVideoHandler = asyncHandler(async (req: Request, res: Response) => {
  const videoId = requireParam(req.params.videoId, "videoId");
  const result = await saveVideo(videoId, requireUserId(req));
  sendData(res, result);
});

export const deleteSaveVideoHandler = asyncHandler(async (req: Request, res: Response) => {
  const videoId = requireParam(req.params.videoId, "videoId");
  const result = await unsaveVideo(videoId, requireUserId(req));
  sendData(res, result);
});

export const getMyProfileHandler = asyncHandler(async (req: Request, res: Response) => {
  const userId = requireUserId(req);
  const profile = await getUserProfile(userId, userId);
  sendData(res, profile);
});

export const patchMyProfileHandler = asyncHandler(async (req: Request, res: Response) => {
  const body = parseBody(updateProfileSchema, req.body);
  const profile = await updateMyProfile(requireUserId(req), body);
  sendData(res, profile);
});

export const postMyAvatarHandler = asyncHandler(async (req: Request, res: Response) => {
  const file = (req as Request & { file?: Express.Multer.File }).file;
  if (!file) {
    throw new AppError(400, "VALIDATION_ERROR", "Avatar image file is required.");
  }

  const publicUrl = `/avatars/${file.filename}`;
  // Absolute URL helps Android Coil load across hosts (emulator/VM).
  const host = req.get("host");
  const proto = (req.headers["x-forwarded-proto"] as string) || req.protocol;
  const absoluteUrl = host ? `${proto}://${host}${publicUrl}` : publicUrl;

  const profile = await updateMyAvatarUrl(requireUserId(req), absoluteUrl);
  sendData(res, profile, 201);
});

export const getUserProfileHandler = asyncHandler(async (req: Request, res: Response) => {
  const userId = requireParam(req.params.userId, "userId");
  const profile = await getUserProfile(userId, req.userId);
  sendData(res, profile);
});

export const getUserVideosHandler = asyncHandler(async (req: Request, res: Response) => {
  const userId = requireParam(req.params.userId, "userId");
  const query = parseQuery(userVideosQuerySchema, req.query);
  const page = await listUserVideos(userId, query);
  sendData(res, page);
});

export const getMyLikedVideosHandler = asyncHandler(async (req: Request, res: Response) => {
  const query = parseQuery(userVideosQuerySchema, req.query);
  const page = await listLikedVideos(requireUserId(req), query);
  sendData(res, page);
});

export const getMySavedVideosHandler = asyncHandler(async (req: Request, res: Response) => {
  const query = parseQuery(userVideosQuerySchema, req.query);
  const page = await listSavedVideos(requireUserId(req), query);
  sendData(res, page);
});

export const getDiscoverHandler = asyncHandler(async (req: Request, res: Response) => {
  const query = parseQuery(discoverQuerySchema, req.query);
  const result = await discover(query);
  sendData(res, result);
});

export const getInboxHandler = asyncHandler(async (req: Request, res: Response) => {
  const query = parseQuery(inboxQuerySchema, req.query);
  const page = await getInbox(requireUserId(req), query);
  sendData(res, page);
});

export const postInboxReadAllHandler = asyncHandler(async (req: Request, res: Response) => {
  await markAllInboxRead(requireUserId(req));
  sendData(res, { success: true });
});

export const postInboxItemReadHandler = asyncHandler(async (req: Request, res: Response) => {
  const id = requireParam(req.params.id, "id");
  await markInboxItemRead(requireUserId(req), id);
  sendData(res, { success: true });
});

export const postDeviceFcmHandler = asyncHandler(async (req: Request, res: Response) => {
  const body = parseBody(registerFcmTokenSchema, req.body);
  await registerFcmToken(requireUserId(req), body);
  sendData(res, { success: true });
});

export const postReportHandler = asyncHandler(async (req: Request, res: Response) => {
  const body = parseBody(createReportSchema, req.body);
  const result = await submitReport(requireUserId(req), body);
  sendData(res, result, 201);
});
