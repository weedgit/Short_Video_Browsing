import { z } from "zod";

export const adminUsersQuerySchema = z.object({
  cursor: z.string().trim().optional(),
  limit: z.coerce.number().int().min(1).max(100).default(20),
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
  cursor: z.string().trim().optional(),
  limit: z.coerce.number().int().min(1).max(100).default(20),
});

export const adminUpdateVideoSchema = z.object({
  status: z.enum(["PROCESSING", "READY", "FAILED", "DELETED"]),
});

export const adminReportsQuerySchema = z.object({
  status: z.enum(["OPEN", "RESOLVED", "DISMISSED"]).optional(),
  cursor: z.string().trim().optional(),
  limit: z.coerce.number().int().min(1).max(100).default(20),
});

export const adminUpdateReportSchema = z.object({
  status: z.enum(["OPEN", "RESOLVED", "DISMISSED"]),
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

export type AdminUsersQueryInput = z.output<typeof adminUsersQuerySchema>;
export type AdminUpdateUserInput = z.output<typeof adminUpdateUserSchema>;
export type AdminVideosQueryInput = z.output<typeof adminVideosQuerySchema>;
export type AdminUpdateVideoInput = z.output<typeof adminUpdateVideoSchema>;
export type AdminReportsQueryInput = z.output<typeof adminReportsQuerySchema>;
export type AdminUpdateReportInput = z.output<typeof adminUpdateReportSchema>;
export type AdminCreateAnnouncementInput = z.output<typeof adminCreateAnnouncementSchema>;
export type AdminUpdateAnnouncementInput = z.output<typeof adminUpdateAnnouncementSchema>;
