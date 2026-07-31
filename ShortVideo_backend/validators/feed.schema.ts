import { z } from "zod";

export const feedQuerySchema = z.object({
  cursor: z.string().trim().optional(),
  limit: z.coerce.number().int().min(1).max(20).default(10),
  tab: z.enum(["foryou", "following"]).default("foryou"),
});

export const playbackEventSchema = z.object({
  videoId: z.string().min(1),
  eventType: z.enum([
    "PLAY",
    "PAUSE",
    "SEEK",
    "PROGRESS",
    "COMPLETE",
    "BUFFER",
    "IMPRESSION",
    "TTFF",
  ]),
  positionMs: z.number().int().min(0).default(0),
  occurredAt: z.string().datetime().optional(),
});

export const playbackBatchSchema = z.object({
  events: z.array(playbackEventSchema).min(1).max(50),
});

export type FeedQueryInput = z.output<typeof feedQuerySchema>;
export type PlaybackBatchInput = z.output<typeof playbackBatchSchema>;
