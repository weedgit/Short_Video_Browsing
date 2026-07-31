export interface AuthUserResponse {
  id: string;
  email: string;
  username: string;
  displayName: string;
  avatarUrl: string | null;
  role: string;
  status: string;
  createdAt: string;
}

export interface AuthTokensResponse {
  accessToken: string;
  refreshToken: string;
  accessTokenExpiresIn: number;
  tokenType: "Bearer";
}

export interface AuthSessionResponse {
  user: AuthUserResponse;
  tokens: AuthTokensResponse;
}

export interface AccessTokenClaims {
  sub: string;
  email: string;
  role: string;
}

declare global {
  namespace Express {
    interface Request {
      userId?: string;
      userEmail?: string;
      userRole?: string;
      rawBody?: Buffer;
    }
  }
}

export {};
