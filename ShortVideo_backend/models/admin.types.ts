export type AdminUserSummary = {
  id: string;
  email: string;
  username: string;
  displayName: string;
  role: string;
  status: string;
  createdAt: string;
};

export type AdminUsersPage = {
  items: AdminUserSummary[];
  nextCursor: string | null;
  hasMore: boolean;
};

export type AdminVideoSummary = {
  id: string;
  userId: string;
  authorName: string;
  description: string;
  status: string;
  thumbnailUrl: string | null;
  likeCount: number;
  createdAt: string;
};

export type AdminVideosPage = {
  items: AdminVideoSummary[];
  nextCursor: string | null;
  hasMore: boolean;
};

export type AdminReportSummary = {
  id: string;
  reporterId: string;
  reporterName: string;
  targetType: string;
  targetId: string;
  reason: string;
  status: string;
  createdAt: string;
  resolvedAt: string | null;
};

export type AdminReportsPage = {
  items: AdminReportSummary[];
  nextCursor: string | null;
  hasMore: boolean;
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

export type AdminAnalytics = {
  userCount: number;
  videoCount: number;
  readyVideoCount: number;
  openReportCount: number;
  likeCount: number;
  commentCount: number;
};
