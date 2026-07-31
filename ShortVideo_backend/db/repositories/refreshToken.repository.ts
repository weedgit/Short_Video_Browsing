import type { RefreshToken } from "@prisma/client";
import { getPrismaClient } from "../client";
import { hashToken } from "../../utils/token";

export async function createRefreshToken(input: {
  userId: string;
  token: string;
  expiresAt: Date;
}): Promise<RefreshToken> {
  return getPrismaClient().refreshToken.create({
    data: {
      userId: input.userId,
      tokenHash: hashToken(input.token),
      expiresAt: input.expiresAt,
    },
  });
}

export async function findValidRefreshToken(token: string): Promise<RefreshToken | null> {
  return getPrismaClient().refreshToken.findFirst({
    where: {
      tokenHash: hashToken(token),
      revokedAt: null,
      expiresAt: { gt: new Date() },
    },
  });
}

export async function revokeRefreshToken(token: string): Promise<void> {
  await getPrismaClient().refreshToken.updateMany({
    where: {
      tokenHash: hashToken(token),
      revokedAt: null,
    },
    data: {
      revokedAt: new Date(),
    },
  });
}

export async function rotateRefreshToken(input: {
  oldToken: string;
  newToken: string;
  newExpiresAt: Date;
  userId: string;
}): Promise<RefreshToken> {
  const prisma = getPrismaClient();

  return prisma.$transaction(async (tx) => {
    const existing = await tx.refreshToken.findFirst({
      where: {
        tokenHash: hashToken(input.oldToken),
        userId: input.userId,
        revokedAt: null,
        expiresAt: { gt: new Date() },
      },
    });

    if (!existing) {
      throw new Error("INVALID_REFRESH_TOKEN");
    }

    const replacement = await tx.refreshToken.create({
      data: {
        userId: input.userId,
        tokenHash: hashToken(input.newToken),
        expiresAt: input.newExpiresAt,
      },
    });

    await tx.refreshToken.update({
      where: { id: existing.id },
      data: {
        revokedAt: new Date(),
        replacedBy: replacement.id,
      },
    });

    return replacement;
  });
}

export async function revokeAllUserRefreshTokens(userId: string): Promise<void> {
  await getPrismaClient().refreshToken.updateMany({
    where: {
      userId,
      revokedAt: null,
    },
    data: {
      revokedAt: new Date(),
    },
  });
}
