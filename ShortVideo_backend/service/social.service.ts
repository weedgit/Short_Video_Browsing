import { AppError } from "../middleware/errorHandler";
import {
  countFollowers,
  countFollowing,
  countUserVideos,
  countUnreadNotifications,
  createComment,
  createNotification,
  createReport,
  findCommentsByVideo,
  findFollowingUserIdsSubset,
  findLikedVideoIds,
  findNotificationsForUser,
  findSavedVideoIds,
  findTrendingHashtags,
  findTrendingVideos,
  findUserPublicProfile,
  findUserVideos as findUserVideosRepo,
  findVideoOwner,
  followUser as followUserRepo,
  isFollowing as isFollowingRepo,
  likeVideo as likeVideoRepo,
  markAllNotificationsRead,
  markNotificationRead,
  saveVideo as saveVideoRepo,
  searchHashtags,
  searchUsers,
  searchVideos,
  sumUserVideoLikes,
  unfollowUser as unfollowUserRepo,
  unlikeVideo as unlikeVideoRepo,
  unsaveVideo as unsaveVideoRepo,
} from "../db/repositories/social.repository";
import { findFcmTokensForUser, upsertFcmToken } from "../db/repositories/userDevice.repository";
import { findUserById } from "../db/repositories/user.repository";
import { sendPushNotification } from "../integrations/push";
import { resolveThumbnailUrl } from "../integrations/cloudflare";
import type {
  CommentsPage,
  DiscoverResult,
  FollowToggleResult,
  InboxPage,
  LikeToggleResult,
  ReportResult,
  SaveToggleResult,
  UserProfile,
  UserVideosPage,
} from "../models/social.types";
import { decodeFeedCursor, encodeFeedCursor } from "../utils/feedCursor";
import type {
  CommentCreateInput,
  CommentsQueryInput,
  CreateReportInput,
  DiscoverQueryInput,
  InboxQueryInput,
  RegisterFcmTokenInput,
  UserVideosQueryInput,
} from "../validators/social.schema";

function decodeCursorSafe(cursor?: string): { createdAt: Date; id: string } | undefined {
  if (!cursor) return undefined;
  try {
    const payload = decodeFeedCursor(cursor);
    return { createdAt: new Date(payload.createdAt), id: payload.id };
  } catch {
    throw new AppError(400, "INVALID_CURSOR", "Invalid pagination cursor.");
  }
}

async function notifyUser(params: {
  userId: string;
  type: string;
  title: string;
  body: string;
  videoId?: string;
  actorUserId?: string;
}): Promise<void> {
  await createNotification(params);

  const fcmTokens = await findFcmTokensForUser(params.userId);
  await sendPushNotification({
    userId: params.userId,
    title: params.title,
    body: params.body,
    fcmTokens,
  });
}

export async function likeVideo(videoId: string, userId: string): Promise<LikeToggleResult> {
  const video = await findVideoOwner(videoId);
  if (!video) {
    throw new AppError(404, "VIDEO_NOT_FOUND", "Video not found.");
  }

  const { likeCount } = await likeVideoRepo(videoId, userId);

  if (video.userId !== userId) {
    await notifyUser({
      userId: video.userId,
      type: "LIKE",
      title: "New like",
      body: "Someone liked your video.",
      videoId,
      actorUserId: userId,
    });
  }

  return { liked: true, likeCount };
}

export async function unlikeVideo(videoId: string, userId: string): Promise<LikeToggleResult> {
  const video = await findVideoOwner(videoId);
  if (!video) {
    throw new AppError(404, "VIDEO_NOT_FOUND", "Video not found.");
  }

  const { likeCount } = await unlikeVideoRepo(videoId, userId);
  return { liked: false, likeCount };
}

export async function listComments(videoId: string, query: CommentsQueryInput): Promise<CommentsPage> {
  const cursor = decodeCursorSafe(query.cursor);
  const rows = await findCommentsByVideo(videoId, query.limit + 1, cursor);
  const hasMore = rows.length > query.limit;
  const page = hasMore ? rows.slice(0, query.limit) : rows;

  const items = page.map((row) => ({
    id: row.id,
    videoId: row.videoId,
    userId: row.userId,
    authorName: row.user.displayName,
    authorAvatarUrl: row.user.avatarUrl,
    text: row.text,
    createdAt: row.createdAt.toISOString(),
  }));

  const last = page.at(-1);
  const nextCursor =
    hasMore && last ? encodeFeedCursor({ createdAt: last.createdAt.toISOString(), id: last.id }) : null;

  return { items, nextCursor, hasMore };
}

export async function createVideoComment(
  videoId: string,
  userId: string,
  input: CommentCreateInput,
) {
  const video = await findVideoOwner(videoId);
  if (!video) {
    throw new AppError(404, "VIDEO_NOT_FOUND", "Video not found.");
  }

  const comment = await createComment({ videoId, userId, text: input.text });

  if (video.userId !== userId) {
    await notifyUser({
      userId: video.userId,
      type: "COMMENT",
      title: "New comment",
      body: input.text.slice(0, 140),
      videoId,
      actorUserId: userId,
    });
  }

  return {
    id: comment.id,
    videoId: comment.videoId,
    userId: comment.userId,
    authorName: comment.user.displayName,
    authorAvatarUrl: comment.user.avatarUrl,
    text: comment.text,
    createdAt: comment.createdAt.toISOString(),
  };
}

export async function followUser(followerId: string, targetUserId: string): Promise<FollowToggleResult> {
  if (followerId === targetUserId) {
    throw new AppError(400, "CANNOT_FOLLOW_SELF", "You cannot follow yourself.");
  }

  const target = await findUserById(targetUserId);
  if (!target || target.deletedAt) {
    throw new AppError(404, "USER_NOT_FOUND", "User not found.");
  }

  await followUserRepo(followerId, targetUserId);
  await notifyUser({
    userId: targetUserId,
    type: "FOLLOW",
    title: "New follower",
    body: "Someone started following you.",
    actorUserId: followerId,
  });

  const followerCount = await countFollowers(targetUserId);
  return { following: true, followerCount };
}

export async function unfollowUser(followerId: string, targetUserId: string): Promise<FollowToggleResult> {
  await unfollowUserRepo(followerId, targetUserId);
  const followerCount = await countFollowers(targetUserId);
  return { following: false, followerCount };
}

export async function saveVideo(videoId: string, userId: string): Promise<SaveToggleResult> {
  const video = await findVideoOwner(videoId);
  if (!video) {
    throw new AppError(404, "VIDEO_NOT_FOUND", "Video not found.");
  }

  await saveVideoRepo(videoId, userId);
  return { saved: true };
}

export async function unsaveVideo(videoId: string, userId: string): Promise<SaveToggleResult> {
  await unsaveVideoRepo(videoId, userId);
  return { saved: false };
}

export async function getUserProfile(targetUserId: string, viewerId?: string): Promise<UserProfile> {
  const user = await findUserPublicProfile(targetUserId);
  if (!user) {
    throw new AppError(404, "USER_NOT_FOUND", "User not found.");
  }

  const [followerCount, followingCount, videoCount, likeCount, viewerFollows] = await Promise.all([
    countFollowers(user.id),
    countFollowing(user.id),
    countUserVideos(user.id),
    sumUserVideoLikes(user.id),
    viewerId && viewerId !== user.id ? isFollowingRepo(viewerId, user.id) : Promise.resolve(false),
  ]);

  return {
    id: user.id,
    username: user.username,
    displayName: user.displayName,
    avatarUrl: user.avatarUrl,
    bio: user.bio,
    followerCount,
    followingCount,
    videoCount,
    likeCount,
    isFollowing: Boolean(viewerFollows),
    isMe: viewerId === user.id,
  };
}

export async function listUserVideos(
  targetUserId: string,
  query: UserVideosQueryInput,
): Promise<UserVideosPage> {
  const cursor = decodeCursorSafe(query.cursor);
  const rows = await findUserVideosRepo(targetUserId, query.limit + 1, cursor);
  const hasMore = rows.length > query.limit;
  const page = hasMore ? rows.slice(0, query.limit) : rows;

  const items = page.map((video) => ({
    id: video.id,
    thumbnailUrl: resolveThumbnailUrl(video),
    likeCount: video.likeCount,
    durationMs: video.durationMs,
  }));

  const last = page.at(-1);
  const nextCursor =
    hasMore && last ? encodeFeedCursor({ createdAt: last.createdAt.toISOString(), id: last.id }) : null;

  return { items, nextCursor, hasMore };
}

export async function discover(query: DiscoverQueryInput): Promise<DiscoverResult> {
  const q = query.q?.trim();

  if (!q) {
    const [hashtags, videos] = await Promise.all([
      findTrendingHashtags(query.limit),
      findTrendingVideos(query.limit),
    ]);

    return {
      hashtags,
      users: [],
      videos: videos.map((video) => ({
        id: video.id,
        description: video.description,
        thumbnailUrl: resolveThumbnailUrl(video),
        likeCount: video.likeCount,
      })),
    };
  }

  const bareQuery = q.replace(/^#/, "");
  const [hashtags, users, videos] = await Promise.all([
    searchHashtags(bareQuery, query.limit),
    searchUsers(q, query.limit),
    searchVideos(q, query.limit),
  ]);

  return {
    hashtags,
    users: users.map((user) => ({
      id: user.id,
      username: user.username,
      displayName: user.displayName,
      avatarUrl: user.avatarUrl,
    })),
    videos: videos.map((video) => ({
      id: video.id,
      description: video.description,
      thumbnailUrl: resolveThumbnailUrl(video),
      likeCount: video.likeCount,
    })),
  };
}

export async function getInbox(userId: string, query: InboxQueryInput): Promise<InboxPage> {
  const cursor = decodeCursorSafe(query.cursor);
  const [rows, unreadCount] = await Promise.all([
    findNotificationsForUser(userId, query.limit + 1, cursor),
    countUnreadNotifications(userId),
  ]);

  const hasMore = rows.length > query.limit;
  const page = hasMore ? rows.slice(0, query.limit) : rows;

  const items = page.map((row) => ({
    id: row.id,
    type: row.type,
    title: row.title,
    body: row.body,
    isRead: row.isRead,
    videoId: row.videoId,
    actorUserId: row.actorUserId,
    actorName: row.actorUser?.displayName ?? null,
    actorAvatarUrl: row.actorUser?.avatarUrl ?? null,
    createdAt: row.createdAt.toISOString(),
  }));

  const last = page.at(-1);
  const nextCursor =
    hasMore && last ? encodeFeedCursor({ createdAt: last.createdAt.toISOString(), id: last.id }) : null;

  return { items, nextCursor, hasMore, unreadCount };
}

export async function markInboxItemRead(userId: string, notificationId: string): Promise<void> {
  await markNotificationRead(notificationId, userId);
}

export async function markAllInboxRead(userId: string): Promise<void> {
  await markAllNotificationsRead(userId);
}

export async function registerFcmToken(userId: string, input: RegisterFcmTokenInput): Promise<void> {
  await upsertFcmToken({
    userId,
    deviceId: input.deviceId,
    fcmToken: input.fcmToken,
    platform: input.platform,
  });
}

export async function submitReport(reporterId: string, input: CreateReportInput): Promise<ReportResult> {
  const report = await createReport({
    reporterId,
    targetType: input.targetType,
    targetId: input.targetId,
    reason: input.reason,
  });

  return { id: report.id, status: report.status };
}
