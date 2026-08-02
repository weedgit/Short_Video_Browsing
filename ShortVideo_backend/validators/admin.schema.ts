import { z } from "zod";

const pageQuery = {
  page: z.coerce.number().int().min(1).default(1),
  limit: z.coerce.number().int().min(1).max(100).default(20),
};

export const adminUsersQuerySchema = z.object({
  q: z.string().trim().max(100).optional(),
  role: z.enum(["USER", "ADMIN"]).optional(),
  status: z.enum(["ACTIVE", "SUSPENDED", "DELETED"]).optional(),
  ...pageQuery,
});

export const adminUpdateUserSchema = z
  .object({
    status: z.enum(["ACTIVE", "SUSPENDED", "DELETED"]).optional(),
    role: z.enum(["USER", "ADMIN"]).optional(),
  })
  .refine((value) => value.status !== undefined || value.role !== undefined, {
    message: "status or role is required.",
  });

export const adminVideosQuerySchema = z.object({
  status: z.enum(["PROCESSING", "READY", "FAILED", "DELETED"]).optional(),
  q: z.string().trim().max(100).optional(),
  hashtag: z.string().trim().max(50).optional(),
  category: z.string().trim().max(50).optional(),
  ...pageQuery,
});

export const adminUpdateVideoSchema = z.object({
  status: z.enum(["PROCESSING", "READY", "FAILED", "DELETED"]),
});

export const adminReportsQuerySchema = z.object({
  status: z.enum(["OPEN", "RESOLVED", "DISMISSED"]).optional(),
  q: z.string().trim().max(100).optional(),
  ...pageQuery,
});

export const adminUpdateReportSchema = z.object({
  status: z.enum(["OPEN", "RESOLVED", "DISMISSED"]),
});

export const adminAnnouncementsQuerySchema = z.object({
  q: z.string().trim().max(100).optional(),
  active: z.enum(["true", "false"]).optional(),
  ...pageQuery,
});

export const adminCreateAnnouncementSchema = z.object({
  title: z.string().trim().min(1, "title is required.").max(200),
  body: z.string().trim().min(1, "body is required.").max(5000),
  publishedAt: z.string().datetime().optional(),
  isActive: z.boolean().default(true),
});

export const adminUpdateAnnouncementSchema = z
  .object({
    title: z.string().trim().min(1, "title is required.").max(200).optional(),
    body: z.string().trim().min(1, "body is required.").max(5000).optional(),
    publishedAt: z.string().datetime().nullable().optional(),
    isActive: z.boolean().optional(),
  })
  .refine(
    (value) =>
      value.title !== undefined ||
      value.body !== undefined ||
      value.publishedAt !== undefined ||
      value.isActive !== undefined,
    { message: "At least one field is required." },
  );

export const adminAnalyticsQuerySchema = z.object({
  range: z.coerce.number().int().refine((value) => value === 7 || value === 30, {
    message: "range must be 7 or 30.",
  }).default(7),
});

export type AdminUsersQueryInput = z.output<typeof adminUsersQuerySchema>;
export type AdminUpdateUserInput = z.output<typeof adminUpdateUserSchema>;
export type AdminVideosQueryInput = z.output<typeof adminVideosQuerySchema>;
export type AdminUpdateVideoInput = z.output<typeof adminUpdateVideoSchema>;
export type AdminReportsQueryInput = z.output<typeof adminReportsQuerySchema>;
export type AdminUpdateReportInput = z.output<typeof adminUpdateReportSchema>;
export type AdminAnnouncementsQueryInput = z.output<typeof adminAnnouncementsQuerySchema>;
export type AdminCreateAnnouncementInput = z.output<typeof adminCreateAnnouncementSchema>;
export type AdminUpdateAnnouncementInput = z.output<typeof adminUpdateAnnouncementSchema>;
export type AdminAnalyticsQueryInput = z.output<typeof adminAnalyticsQuerySchema>;
