import { z } from "zod";
import {
  ALIBABA_VOD_VIDEO_FORMAT_HINT,
  isAlibabaVodVideoMimeType,
} from "../utils/vod-video-formats";

/** MIME must be an Alibaba ApsaraVideo VOD supported video type. */
export const createUploadSchema = z.object({
  mimeType: z
    .string()
    .trim()
    .min(1)
    .refine((value) => isAlibabaVodVideoMimeType(value), {
      message: `Unsupported video type. Use an Alibaba VOD format (${ALIBABA_VOD_VIDEO_FORMAT_HINT}).`,
    }),
  fileSizeBytes: z.coerce.number().int().positive(),
  durationMs: z.coerce.number().int().nonnegative().max(600_000).optional(),
});

/** Fixed publish categories (aligned with Android VideoCategories). */
export const VIDEO_CATEGORIES = [
  "Comedy",
  "Dance",
  "Beauty & Style",
  "Food",
  "Sports",
  "Animals",
  "Gaming",
  "Music",
  "Fashion",
  "Education",
  "Travel",
  "DIY & Life Hacks",
  "Autos",
  "Science & Tech",
  "Entertainment",
  "Family",
  "Art",
  "Fitness & Health",
  "ASMR",
  "News & Politics",
] as const;

export const publishVideoSchema = z.object({
  description: z.string().trim().min(1).max(500),
  hashtags: z.array(z.string().trim().min(1).max(50)).max(10).default([]),
  category: z.enum(VIDEO_CATEGORIES).optional(),
});

export const uploadProgressSchema = z.object({
  bytesUploaded: z.coerce.number().int().nonnegative(),
});

export type CreateUploadInput = z.output<typeof createUploadSchema>;
export type PublishVideoInput = z.output<typeof publishVideoSchema>;
export type UploadProgressInput = z.output<typeof uploadProgressSchema>;
