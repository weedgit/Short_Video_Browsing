export type LikeToggleResult = {
  liked: boolean;
  likeCount: number;
};

export type CommentItem = {
  id: string;
  videoId: string;
  userId: string;
  authorName: string;
  authorAvatarUrl: string | null;
  text: string;
  createdAt: string;
  parentId: string | null;
  replyToAuthorName: string | null;
  replyCount: number;
  replies: CommentItem[];
};

export type CommentsPage = {
  items: CommentItem[];
  nextCursor: string | null;
  hasMore: boolean;
};

export type FollowToggleResult = {
  following: boolean;
  followerCount: number;
};

export type SaveToggleResult = {
  saved: boolean;
};

export type UserProfile = {
  id: string;
  username: string;
  displayName: string;
  avatarUrl: string | null;
  bio: string | null;
  followerCount: number;
  followingCount: number;
  videoCount: number;
  likeCount: number;
  isFollowing: boolean;
  isMe: boolean;
  /** Android client field (same as isMe). */
  isSelf: boolean;
};

export type UserVideoSummary = {
  id: string;
  thumbnailUrl: string | null;
  likeCount: number;
  durationMs: number;
};

export type UserVideosPage = {
  items: UserVideoSummary[];
  nextCursor: string | null;
  hasMore: boolean;
};

export type DiscoverHashtag = {
  tag: string;
  videoCount: number;
};

export type DiscoverUser = {
  id: string;
  username: string;
  displayName: string;
  avatarUrl: string | null;
  followerCount?: number;
  isFollowing?: boolean;
};

export type DiscoverVideo = {
  id: string;
  description: string;
  thumbnailUrl: string | null;
  likeCount: number;
  authorId: string | null;
  authorName: string;
  authorAvatarUrl: string | null;
};

export type DiscoverResult = {
  hashtags: DiscoverHashtag[];
  users: DiscoverUser[];
  videos: DiscoverVideo[];
};

export type NotificationItem = {
  id: string;
  type: string;
  title: string;
  body: string;
  isRead: boolean;
  videoId: string | null;
  actorUserId: string | null;
  actorName: string | null;
  actorAvatarUrl: string | null;
  createdAt: string;
};

export type InboxPage = {
  items: NotificationItem[];
  nextCursor: string | null;
  hasMore: boolean;
  unreadCount: number;
};

export type ReportResult = {
  id: string;
  status: string;
};
