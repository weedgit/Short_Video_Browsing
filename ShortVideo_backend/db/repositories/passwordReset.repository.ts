import type { PasswordResetToken } from "@prisma/client";
import { getPrismaClient } from "../client";
import { hashToken } from "../../utils/token";

export async function createPasswordResetToken(input: {
  userId: string;
  token: string;
  expiresAt: Date;
}): Promise<PasswordResetToken> {
  await getPrismaClient().passwordResetToken.updateMany({
    where: {
      userId: input.userId,
      usedAt: null,
    },
    data: {
      usedAt: new Date(),
    },
  });

  return getPrismaClient().passwordResetToken.create({
    data: {
      userId: input.userId,
      tokenHash: hashToken(input.token),
      expiresAt: input.expiresAt,
    },
  });
}

export async function findValidPasswordResetToken(token: string): Promise<PasswordResetToken | null> {
  return getPrismaClient().passwordResetToken.findFirst({
    where: {
      tokenHash: hashToken(token),
      usedAt: null,
      expiresAt: { gt: new Date() },
    },
  });
}

export async function markPasswordResetTokenUsed(token: string): Promise<void> {
  await getPrismaClient().passwordResetToken.updateMany({
    where: {
      tokenHash: hashToken(token),
      usedAt: null,
    },
    data: {
      usedAt: new Date(),
    },
  });
}

export async function revokeAllUserPasswordResetTokens(userId: string): Promise<void> {
  await getPrismaClient().passwordResetToken.updateMany({
    where: {
      userId,
      usedAt: null,
    },
    data: {
      usedAt: new Date(),
    },
  });
}
