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
  thumbnailUrl: string | null;
  authorId: string;
  authorName: string;
  authorAvatarUrl: string | null;
  description: string;
  hashtags: string[];
  category: string | null;
  uploadedAtLabel: string;
  durationMs: number;
  likeCount: number;
  commentCount: number;
  shareCount: number;
  isLiked: boolean;
  isFollowing: boolean;
  isSaved: boolean;
  musicLabel: string | null;
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
