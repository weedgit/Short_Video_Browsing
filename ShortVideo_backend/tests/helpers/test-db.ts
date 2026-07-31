import { getPrismaClient } from "../../db/client";

export async function resetAuthTestData(): Promise<void> {
  const prisma = getPrismaClient();
  await prisma.playbackEvent.deleteMany();
  await prisma.feedImpression.deleteMany();
  await prisma.videoHashtag.deleteMany();
  await prisma.webhookEvent.deleteMany();
  await prisma.uploadSession.deleteMany();
  await prisma.video.deleteMany();
  await prisma.passwordResetToken.deleteMany();
  await prisma.refreshToken.deleteMany();
  await prisma.userDevice.deleteMany();
  await prisma.user.deleteMany();
}

export async function resetFeedTestData(): Promise<void> {
  const prisma = getPrismaClient();
  await prisma.playbackEvent.deleteMany();
  await prisma.feedImpression.deleteMany();
  await prisma.videoHashtag.deleteMany();
  await prisma.webhookEvent.deleteMany();
  await prisma.uploadSession.deleteMany();
  await prisma.video.deleteMany();
}

export function hasDatabaseConfigured(): boolean {
  return Boolean(process.env.DATABASE_URL);
}

export async function canConnectToDatabase(): Promise<boolean> {
  if (!hasDatabaseConfigured()) {
    return false;
  }

  try {
    await getPrismaClient().$queryRaw`SELECT 1`;
    return true;
  } catch {
    return false;
  }
}
