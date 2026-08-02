export type AdminUserSummary = {
  id: string;
  email: string;
  username: string;
  displayName: string;
  avatarUrl: string | null;
  bio: string | null;
  role: string;
  status: string;
  createdAt: string;
};

export type AdminPageMeta = {
  page: number;
  limit: number;
  total: number;
  totalPages: number;
  hasMore: boolean;
};

export type AdminUsersPage = AdminPageMeta & {
  items: AdminUserSummary[];
};

export type AdminVideoSummary = {
  id: string;
  userId: string;
  authorName: string;
  authorUsername: string;
  authorAvatarUrl: string | null;
  description: string;
  status: string;
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

export type AdminVideosPage = AdminPageMeta & {
  items: AdminVideoSummary[];
};

export type AdminUserProfile = {
  id: string;
  email: string;
  username: string;
  displayName: string;
  avatarUrl: string | null;
  bio: string | null;
  role: string;
  status: string;
  createdAt: string;
  followerCount: number;
  followingCount: number;
  videoCount: number;
  likeCount: number;
};

export type AdminReportSummary = {
  id: string;
  reporterId: string;
  reporterName: string;
  targetType: string;
  targetId: string;
  reason: string;
  title: string;
  message: string;
  status: string;
  createdAt: string;
  resolvedAt: string | null;
};

export type AdminReportsPage = AdminPageMeta & {
  items: AdminReportSummary[];
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

export type AdminAnnouncementsPage = AdminPageMeta & {
  items: AdminAnnouncement[];
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
