import { AppError } from "../middleware/errorHandler";
import type { FeedAudience, FeedPage, PlaybackBatchResult } from "../models/feed.types";
import {
  clearFeedImpressions,
  countReadyVideos,
  createFeedImpressions,
  createPlaybackEvents,
  findFeedVideos,
  findImpressedVideoIds,
  findVideoIdsByIds,
  ensureDevFeedSeed,
} from "../db/repositories/video.repository";
import {
  findFollowingUserIds,
  findFollowingUserIdsSubset,
  findLikedVideoIds,
  findSavedVideoIds,
} from "../db/repositories/social.repository";
import { resolveSignedPlaybackUrl, resolveThumbnailUrl } from "../integrations/cloudflare";
import { decodeFeedCursor, encodeFeedCursor, formatUploadedAtLabel } from "../utils/feedCursor";
import { getCachedForYouFirstPage, setCachedForYouFirstPage } from "../utils/feedCache";
import type { FeedQueryInput, PlaybackBatchInput } from "../validators/feed.schema";

async function ensureDevSeedData(): Promise<void> {
  await ensureDevFeedSeed();
}

export async function getFeedPage(
  query: FeedQueryInput,
  audience: FeedAudience,
): Promise<FeedPage> {
  await ensureDevSeedData();

  const isCacheableFirstPage = query.tab === "foryou" && !query.cursor;
  const cacheKey = isCacheableFirstPage ? audience.userId ?? audience.deviceId ?? null : null;

  if (cacheKey) {
    const cached = await getCachedForYouFirstPage(cacheKey);
    if (cached) return cached;
  }

  let followingUserIds: string[] | undefined;
  if (query.tab === "following") {
    if (!audience.userId) {
      return { items: [], nextCursor: null, hasMore: false };
    }

    followingUserIds = await findFollowingUserIds(audience.userId);
    if (followingUserIds.length === 0) {
      return { items: [], nextCursor: null, hasMore: false };
    }
  }

  const readyCount = await countReadyVideos(followingUserIds);
  if (readyCount === 0) {
    return { items: [], nextCursor: null, hasMore: false };
  }

  let excludeVideoIds = await findImpressedVideoIds(audience);
  if (excludeVideoIds.length >= readyCount) {
    await clearFeedImpressions(audience);
    excludeVideoIds = [];
  }

  const cursor = query.cursor ? decodeFeedCursorSafe(query.cursor) : undefined;
  const fetchLimit = query.limit + 1;

  let videos = await findFeedVideos({
    limit: fetchLimit,
    cursor,
    excludeVideoIds,
    followingUserIds,
  });

  if (videos.length === 0 && excludeVideoIds.length > 0) {
    await clearFeedImpressions(audience);
    videos = await findFeedVideos({
      limit: fetchLimit,
      cursor,
      excludeVideoIds: [],
      followingUserIds,
    });
  }

  const hasMore = videos.length > query.limit;
  const pageVideos = hasMore ? videos.slice(0, query.limit) : videos;

  if (pageVideos.length > 0) {
    await createFeedImpressions({
      videoIds: pageVideos.map((video) => video.id),
      userId: audience.userId,
      deviceId: audience.deviceId,
    });
  }

  const viewerId = audience.userId;
  const videoIds = pageVideos.map((video) => video.id);
  const authorIds = [...new Set(pageVideos.map((video) => video.userId))];

  const [likedSet, savedSet, followingSet] = viewerId
    ? await Promise.all([
        findLikedVideoIds(viewerId, videoIds),
        findSavedVideoIds(viewerId, videoIds),
        findFollowingUserIdsSubset(viewerId, authorIds),
      ])
    : [new Set<string>(), new Set<string>(), new Set<string>()];

  const items = pageVideos.flatMap((video) => {
    let signed;
    try {
      signed = resolveSignedPlaybackUrl({
        cloudflareAssetId: video.cloudflareAssetId,
        hlsUrl: video.hlsUrl,
        streamUrl: video.streamUrl,
      });
    } catch {
      return [];
    }

    return [{
      id: video.id,
      streamUrl: signed.url,
      playbackFormat: signed.format,
      streamUrlExpiresAt: signed.expiresAt.toISOString(),
      thumbnailUrl: resolveThumbnailUrl(video),
      authorId: video.userId,
      authorName: video.user.displayName,
      authorAvatarUrl: video.user.avatarUrl,
      description: video.description,
      hashtags: video.hashtags.map((tag) => tag.tag),
      category: video.category,
      uploadedAtLabel: formatUploadedAtLabel(video.createdAt),
      durationMs: video.durationMs,
      likeCount: video.likeCount,
      commentCount: video.commentCount,
      shareCount: video.shareCount,
      isLiked: likedSet.has(video.id),
      isFollowing: followingSet.has(video.userId),
      isSaved: savedSet.has(video.id),
      musicLabel: video.musicLabel,
    }];
  });

  const lastVideo = pageVideos.at(-1);
  const nextCursor =
    hasMore && lastVideo
      ? encodeFeedCursor({
          createdAt: lastVideo.createdAt.toISOString(),
          id: lastVideo.id,
        })
      : null;

  const page: FeedPage = {
    items,
    nextCursor,
    hasMore,
  };

  if (cacheKey) {
    await setCachedForYouFirstPage(cacheKey, page);
  }

  return page;
}

export async function ingestPlaybackEvents(
  body: PlaybackBatchInput,
  audience: FeedAudience,
): Promise<PlaybackBatchResult> {
  const videoIds = [...new Set(body.events.map((event) => event.videoId))];
  const validVideoIds = new Set(await findVideoIdsByIds(videoIds));

  const events = body.events
    .filter((event) => validVideoIds.has(event.videoId))
    .map((event) => ({
      videoId: event.videoId,
      userId: audience.userId,
      deviceId: audience.deviceId,
      eventType: event.eventType,
      positionMs: event.positionMs,
      occurredAt: event.occurredAt ? new Date(event.occurredAt) : new Date(),
    }));

  const accepted = await createPlaybackEvents(events);
  return { accepted };
}

function decodeFeedCursorSafe(cursor: string) {
  try {
    const payload = decodeFeedCursor(cursor);
    return {
      createdAt: new Date(payload.createdAt),
      id: payload.id,
    };
  } catch {
    throw new AppError(400, "INVALID_CURSOR", "Invalid feed cursor.");
  }
}
