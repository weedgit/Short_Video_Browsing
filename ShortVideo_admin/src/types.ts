export type AdminUser = {
  id: string;
  email: string;
  username: string;
  displayName: string;
  avatarUrl: string | null;
  bio?: string | null;
  role: "USER" | "ADMIN";
  status: "ACTIVE" | "SUSPENDED" | "DELETED";
  createdAt: string;
};

export type AdminVideo = {
  id: string;
  userId: string;
  authorName: string;
  authorUsername: string;
  authorAvatarUrl: string | null;
  description: string;
  status: "PROCESSING" | "READY" | "FAILED" | "DELETED";
  thumbnailUrl: string | null;
  streamUrl: string | null;
  playbackFormat: "hls" | "mp4" | null;
  likeCount: number;
  commentCount: number;
  shareCount: number;
  durationMs: number;
  category: string | null;
  hashtags: string[];
  musicLabel: string | null;
  createdAt: string;
};

export type AdminUserProfile = {
  id: string;
  email: string;
  username: string;
  displayName: string;
  avatarUrl: string | null;
  bio: string | null;
  role: "USER" | "ADMIN";
  status: "ACTIVE" | "SUSPENDED" | "DELETED";
  createdAt: string;
  followerCount: number;
  followingCount: number;
  videoCount: number;
  likeCount: number;
};

export type VideoListFilters = {
  status?: AdminVideo["status"];
  q?: string;
  hashtag?: string;
  category?: string;
};

export type AdminReport = {
  id: string;
  reporterId: string;
  reporterName: string;
  targetType: "VIDEO" | "USER" | "COMMENT";
  targetId: string;
  reason: string;
  title?: string;
  message?: string;
  status: "OPEN" | "RESOLVED" | "DISMISSED";
  createdAt: string;
  resolvedAt: string | null;
};

export type AdminAnnouncement = {
  id: string;
  title: string;
  body: string;
  isActive: boolean;
  publishedAt: string | null;
  createdAt: string;
  createdById: string | null;
};

export type AnalyticsDailyPoint = {
  date: string;
  count: number;
};

export type AnalyticsTrend = {
  currentPeriodTotal: number;
  previousPeriodTotal: number;
  changePercent: number | null;
  direction: "up" | "down" | "flat";
};

export type AdminAnalytics = {
  userCount: number;
  videoCount: number;
  readyVideoCount: number;
  openReportCount: number;
  likeCount: number;
  commentCount: number;
  rangeDays: number;
  series: {
    users: AnalyticsDailyPoint[];
    videos: AnalyticsDailyPoint[];
    likes: AnalyticsDailyPoint[];
    comments: AnalyticsDailyPoint[];
    reports: AnalyticsDailyPoint[];
  };
  trends: {
    users: AnalyticsTrend;
    videos: AnalyticsTrend;
    likes: AnalyticsTrend;
    comments: AnalyticsTrend;
    reports: AnalyticsTrend;
  };
};

export type Page<T> = {
  items: T[];
  page: number;
  limit: number;
  total: number;
  totalPages: number;
  hasMore: boolean;
};

export type UserListFilters = {
  q?: string;
  role?: AdminUser["role"];
  status?: AdminUser["status"];
};

export type ReportListFilters = {
  status?: AdminReport["status"];
  q?: string;
};

export type AnnouncementListFilters = {
  q?: string;
  active?: "true" | "false";
};

export type AuthUser = {
  id: string;
  email: string;
  username: string;
  displayName: string;
  avatarUrl?: string | null;
  role: string;
  status: string;
};

export type AuthSession = {
  user: AuthUser;
  tokens: {
    accessToken: string;
    refreshToken: string;
    accessTokenExpiresIn: number;
    tokenType: "Bearer";
  };
};
