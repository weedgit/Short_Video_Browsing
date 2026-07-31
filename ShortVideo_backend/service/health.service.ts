import type { ApiInfo, HealthCheckResult, ReadinessCheckResult } from "../models";
import { getPrismaClient } from "../db/client";
import { config } from "../config";

export function getLiveHealth(): HealthCheckResult {
  return {
    status: "ok",
    uptimeSeconds: Math.floor(process.uptime()),
    timestamp: new Date().toISOString(),
    version: process.env.npm_package_version ?? "0.1.0",
  };
}

async function checkDatabase(): Promise<ReadinessCheckResult["checks"]["database"]> {
  if (!config.databaseUrl) {
    return "degraded";
  }

  try {
    await getPrismaClient().$queryRaw`SELECT 1`;
    return "ok";
  } catch {
    return "down";
  }
}

export async function getReadyHealth(): Promise<ReadinessCheckResult> {
  const live = getLiveHealth();
  const database = await checkDatabase();
  const redis: ReadinessCheckResult["checks"]["redis"] = config.redisUrl ? "degraded" : "degraded";

  const status =
    database === "ok" ? "ok" : database === "degraded" ? "degraded" : "down";

  return {
    ...live,
    status,
    checks: {
      database,
      redis,
    },
  };
}

export function getApiInfo(apiVersion: string): ApiInfo {
  return {
    name: "shortvideo-backend",
    version: process.env.npm_package_version ?? "0.1.0",
    apiVersion,
  };
}
