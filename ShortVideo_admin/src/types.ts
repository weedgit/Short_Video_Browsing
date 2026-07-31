export type AdminUser = {
  id: string;
  email: string;
  username: string;
  displayName: string;
  role: "USER" | "ADMIN";
  status: "ACTIVE" | "SUSPENDED" | "DELETED";
  createdAt: string;
};

export type AdminVideo = {
  id: string;
  userId: string;
  authorName: string;
  description: string;
  status: "PROCESSING" | "READY" | "FAILED" | "DELETED";
  thumbnailUrl: string | null;
  likeCount: number;
  createdAt: string;
};

export type AdminReport = {
  id: string;
  reporterId: string;
  reporterName: string;
  targetType: "VIDEO" | "USER" | "COMMENT";
  targetId: string;
  reason: string;
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

export type AdminAnalytics = {
  userCount: number;
  videoCount: number;
  readyVideoCount: number;
  openReportCount: number;
  likeCount: number;
  commentCount: number;
};

export type Page<T> = {
  items: T[];
  nextCursor: string | null;
  hasMore: boolean;
};

export type AuthUser = {
  id: string;
  email: string;
  username: string;
  displayName: string;
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
