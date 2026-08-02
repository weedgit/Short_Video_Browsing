import { z } from "zod";

export const commentCreateSchema = z.object({
  text: z.string().trim().min(1, "Comment text is required.").max(500, "Comment is too long."),
  parentId: z.string().trim().min(1).optional().nullable(),
});

export const commentsQuerySchema = z.object({
  cursor: z.string().trim().optional(),
  limit: z.coerce.number().int().min(1).max(50).default(20),
});

export const discoverQuerySchema = z.object({
  q: z.string().trim().max(100).optional(),
  limit: z.coerce.number().int().min(1).max(50).default(20),
});

export const userVideosQuerySchema = z.object({
  cursor: z.string().trim().optional(),
  limit: z.coerce.number().int().min(1).max(50).default(20),
});

export const inboxQuerySchema = z.object({
  cursor: z.string().trim().optional(),
  limit: z.coerce.number().int().min(1).max(50).default(20),
});

export const registerFcmTokenSchema = z.object({
  deviceId: z.string().trim().min(1, "deviceId is required."),
  fcmToken: z.string().trim().min(1, "fcmToken is required."),
  platform: z.string().trim().max(20).optional(),
});

export const createReportSchema = z.object({
  targetType: z.enum(["VIDEO", "USER", "COMMENT"]),
  targetId: z.string().trim().min(1, "targetId is required."),
  reason: z.string().trim().min(1, "reason is required.").max(500),
});

export const updateProfileSchema = z.object({
  displayName: z.string().trim().min(1).max(50).optional(),
  bio: z.string().trim().max(200).optional().nullable(),
  avatarUrl: z.string().trim().url().max(2048).optional().nullable(),
});

export type CommentCreateInput = z.output<typeof commentCreateSchema>;
export type CommentsQueryInput = z.output<typeof commentsQuerySchema>;
export type UpdateProfileInput = z.output<typeof updateProfileSchema>;
export type DiscoverQueryInput = z.output<typeof discoverQuerySchema>;
export type UserVideosQueryInput = z.output<typeof userVideosQuerySchema>;
export type InboxQueryInput = z.output<typeof inboxQuerySchema>;
export type RegisterFcmTokenInput = z.output<typeof registerFcmTokenSchema>;
export type CreateReportInput = z.output<typeof createReportSchema>;
