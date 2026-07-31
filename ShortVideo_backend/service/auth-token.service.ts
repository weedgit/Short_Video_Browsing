import jwt, { type SignOptions } from "jsonwebtoken";
import { config } from "../config";
import type { AccessTokenClaims, AuthSessionResponse, AuthUserResponse } from "../models/auth.types";
import type { User } from "@prisma/client";
import { addDurationToDate, parseDurationToSeconds } from "../utils/duration";
import { generateSecureToken } from "../utils/token";
import {
  createRefreshToken,
  rotateRefreshToken,
} from "../db/repositories/refreshToken.repository";

export function toAuthUserResponse(user: User): AuthUserResponse {
  return {
    id: user.id,
    email: user.email,
    username: user.username,
    displayName: user.displayName,
    avatarUrl: user.avatarUrl,
    role: user.role,
    status: user.status,
    createdAt: user.createdAt.toISOString(),
  };
}

export function signAccessToken(user: User): string {
  const payload: AccessTokenClaims = {
    sub: user.id,
    email: user.email,
    role: user.role,
  };

  const signOptions: SignOptions = {
    expiresIn: config.jwt.accessExpiresIn as SignOptions["expiresIn"],
  };

  return jwt.sign(payload, config.jwt.accessSecret, signOptions);
}

export function verifyAccessToken(token: string): AccessTokenClaims {
  const payload = jwt.verify(token, config.jwt.accessSecret);

  if (typeof payload === "string" || !payload.sub || !payload.email || !payload.role) {
    throw new Error("INVALID_ACCESS_TOKEN");
  }

  return {
    sub: payload.sub,
    email: String(payload.email),
    role: String(payload.role),
  };
}

export async function issueAuthSession(user: User): Promise<AuthSessionResponse> {
  const accessToken = signAccessToken(user);
  const refreshToken = generateSecureToken();
  const refreshExpiresAt = addDurationToDate(config.jwt.refreshExpiresIn);

  await createRefreshToken({
    userId: user.id,
    token: refreshToken,
    expiresAt: refreshExpiresAt,
  });

  return {
    user: toAuthUserResponse(user),
    tokens: {
      accessToken,
      refreshToken,
      accessTokenExpiresIn: parseDurationToSeconds(config.jwt.accessExpiresIn),
      tokenType: "Bearer",
    },
  };
}

export async function rotateAuthSession(user: User, oldRefreshToken: string): Promise<AuthSessionResponse> {
  const accessToken = signAccessToken(user);
  const refreshToken = generateSecureToken();
  const refreshExpiresAt = addDurationToDate(config.jwt.refreshExpiresIn);

  await rotateRefreshToken({
    oldToken: oldRefreshToken,
    newToken: refreshToken,
    newExpiresAt: refreshExpiresAt,
    userId: user.id,
  });

  return {
    user: toAuthUserResponse(user),
    tokens: {
      accessToken,
      refreshToken,
      accessTokenExpiresIn: parseDurationToSeconds(config.jwt.accessExpiresIn),
      tokenType: "Bearer",
    },
  };
}
