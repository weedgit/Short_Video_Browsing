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
