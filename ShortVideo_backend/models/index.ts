export type HealthStatus = "ok" | "degraded" | "down";

export interface HealthCheckResult {
  status: HealthStatus;
  uptimeSeconds: number;
  timestamp: string;
  version: string;
}

export interface ReadinessCheckResult extends HealthCheckResult {
  checks: {
    database: HealthStatus;
    redis: HealthStatus;
  };
}

export interface ApiInfo {
  name: string;
  version: string;
  apiVersion: string;
}
