import { z } from "zod";

/** Any video/* MIME — container may still be rejected by Cloudflare Stream if unsupported. */
export const createUploadSchema = z.object({
  mimeType: z
    .string()
    .trim()
    .min(1)
    .refine((value) => value.toLowerCase().startsWith("video/"), {
      message: "Only video MIME types are allowed.",
    }),
  fileSizeBytes: z.coerce.number().int().positive(),
  durationMs: z.coerce.number().int().nonnegative().max(600_000).optional(),
});

export const publishVideoSchema = z.object({
  description: z.string().trim().min(1).max(500),
  hashtags: z.array(z.string().trim().min(1).max(50)).max(10).default([]),
  category: z.string().trim().max(50).optional(),
});

export const uploadProgressSchema = z.object({
  bytesUploaded: z.coerce.number().int().nonnegative(),
});

export type CreateUploadInput = z.output<typeof createUploadSchema>;
export type PublishVideoInput = z.output<typeof publishVideoSchema>;
export type UploadProgressInput = z.output<typeof uploadProgressSchema>;
