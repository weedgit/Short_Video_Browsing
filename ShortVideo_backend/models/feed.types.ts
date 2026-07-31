export type FeedAudience = {
  userId?: string;
  deviceId?: string;
};

export type FeedCursorPayload = {
  createdAt: string;
  id: string;
};

export type FeedVideoItem = {
  id: string;
  streamUrl: string;
  playbackFormat: "hls" | "mp4";
  streamUrlExpiresAt: string;
  authorName: string;
  description: string;
  hashtags: string[];
  category: string | null;
  uploadedAtLabel: string;
  durationMs: number;
};

export type FeedPage = {
  items: FeedVideoItem[];
  nextCursor: string | null;
  hasMore: boolean;
};

export type SignedPlaybackUrl = {
  url: string;
  expiresAt: Date;
  format: "hls" | "mp4";
};

export type PlaybackEventInput = {
  videoId: string;
  eventType: string;
  positionMs: number;
  occurredAt: Date;
};

export type PlaybackBatchResult = {
  accepted: number;
};
