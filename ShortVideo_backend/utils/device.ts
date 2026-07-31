import type { Request } from "express";

export type ClientDeviceInfo = {
  deviceId?: string;
  platform?: string;
  appVersion?: string;
};

export function getClientDeviceInfo(req: Request): ClientDeviceInfo {
  const deviceId = req.header("x-device-id")?.trim();
  const platform = req.header("x-platform")?.trim();
  const appVersion = req.header("x-app-version")?.trim();

  return {
    deviceId: deviceId || undefined,
    platform: platform || undefined,
    appVersion: appVersion || undefined,
  };
}
