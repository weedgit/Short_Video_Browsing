import { getPrismaClient } from "../client";

export type UpsertUserDeviceInput = {
  userId: string;
  deviceId: string;
  platform?: string;
  appVersion?: string;
};

export async function upsertUserDevice(input: UpsertUserDeviceInput): Promise<void> {
  const prisma = getPrismaClient();

  await prisma.userDevice.upsert({
    where: {
      userId_deviceId: {
        userId: input.userId,
        deviceId: input.deviceId,
      },
    },
    create: {
      userId: input.userId,
      deviceId: input.deviceId,
      platform: input.platform ?? "android",
      appVersion: input.appVersion,
      lastSeenAt: new Date(),
    },
    update: {
      platform: input.platform ?? "android",
      appVersion: input.appVersion,
      lastSeenAt: new Date(),
    },
  });
}

export async function revokeUserDevices(userId: string): Promise<void> {
  const prisma = getPrismaClient();
  await prisma.userDevice.deleteMany({ where: { userId } });
}

export type UpsertFcmTokenInput = {
  userId: string;
  deviceId: string;
  fcmToken: string;
  platform?: string;
};

export async function upsertFcmToken(input: UpsertFcmTokenInput): Promise<void> {
  const prisma = getPrismaClient();

  await prisma.userDevice.upsert({
    where: {
      userId_deviceId: {
        userId: input.userId,
        deviceId: input.deviceId,
      },
    },
    create: {
      userId: input.userId,
      deviceId: input.deviceId,
      platform: input.platform ?? "android",
      fcmToken: input.fcmToken,
      lastSeenAt: new Date(),
    },
    update: {
      platform: input.platform ?? undefined,
      fcmToken: input.fcmToken,
      lastSeenAt: new Date(),
    },
  });
}

export async function findFcmTokensForUser(userId: string): Promise<string[]> {
  const prisma = getPrismaClient();
  const rows = await prisma.userDevice.findMany({
    where: { userId, fcmToken: { not: null } },
    select: { fcmToken: true },
  });

  return rows
    .map((row) => row.fcmToken)
    .filter((token): token is string => Boolean(token));
}
