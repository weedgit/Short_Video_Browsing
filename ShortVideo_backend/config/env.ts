import { z } from "zod";

const envSchema = z.object({
  NODE_ENV: z.enum(["development", "test", "production"]).default("development"),
  PORT: z.coerce.number().int().positive().default(3000),
  API_VERSION: z.string().default("v1"),
  DATABASE_URL: z.string().optional(),
  REDIS_URL: z.string().optional(),
  CORS_ORIGIN: z.string().default("*"),
  LOG_LEVEL: z.enum(["fatal", "error", "warn", "info", "debug", "trace"]).default("info"),
  JWT_ACCESS_SECRET: z.string().min(16).default("dev-access-secret-change-me"),
  JWT_REFRESH_SECRET: z.string().min(16).default("dev-refresh-secret-change-me"),
  JWT_ACCESS_EXPIRES_IN: z.string().default("15m"),
  JWT_REFRESH_EXPIRES_IN: z.string().default("7d"),
  // Alibaba Cloud ApsaraVideo VOD + CDN (China)
  ALIBABA_ACCESS_KEY_ID: z.string().optional(),
  ALIBABA_ACCESS_KEY_SECRET: z.string().optional(),
  ALIBABA_VOD_REGION_ID: z.string().default("cn-shanghai"),
  ALIBABA_VOD_CDN_DOMAIN: z.string().optional(),
  ALIBABA_VOD_TEMPLATE_GROUP_ID: z.string().optional(),
  ALIBABA_VOD_CALLBACK_AUTH_KEY: z.string().optional(),
  FEED_SIGNED_URL_TTL_SECONDS: z.coerce.number().int().positive().default(3600),
  UPLOAD_SIGNED_URL_TTL_SECONDS: z.coerce.number().int().positive().default(3600),
  UPLOAD_MAX_FILE_SIZE_BYTES: z.coerce.number().int().positive().default(1_073_741_824),
  UPLOAD_MAX_DURATION_MS: z.coerce.number().int().positive().default(600_000),
  UPLOAD_MAX_CONCURRENT_PER_USER: z.coerce.number().int().positive().default(1),
});

export type Env = z.infer<typeof envSchema>;

function loadEnv(): Env {
  const parsed = envSchema.safeParse(process.env);

  if (!parsed.success) {
    console.error("Invalid environment configuration:", parsed.error.flatten().fieldErrors);
    process.exit(1);
  }

  if (parsed.data.NODE_ENV === "production") {
    if (parsed.data.JWT_ACCESS_SECRET === "dev-access-secret-change-me") {
      console.error("JWT_ACCESS_SECRET must be set in production");
      process.exit(1);
    }
    if (parsed.data.JWT_REFRESH_SECRET === "dev-refresh-secret-change-me") {
      console.error("JWT_REFRESH_SECRET must be set in production");
      process.exit(1);
    }
  }

  return parsed.data;
}

export const env = loadEnv();

export const config = {
  env: env.NODE_ENV,
  port: env.PORT,
  apiVersion: env.API_VERSION,
  databaseUrl: env.DATABASE_URL,
  redisUrl: env.REDIS_URL,
  corsOrigin: env.CORS_ORIGIN,
  logLevel: env.LOG_LEVEL,
  isProduction: env.NODE_ENV === "production",
  jwt: {
    accessSecret: env.JWT_ACCESS_SECRET,
    refreshSecret: env.JWT_REFRESH_SECRET,
    accessExpiresIn: env.JWT_ACCESS_EXPIRES_IN,
    refreshExpiresIn: env.JWT_REFRESH_EXPIRES_IN,
  },
  alibaba: {
    accessKeyId: env.ALIBABA_ACCESS_KEY_ID,
    accessKeySecret: env.ALIBABA_ACCESS_KEY_SECRET,
    vodRegionId: env.ALIBABA_VOD_REGION_ID,
    vodCdnDomain: env.ALIBABA_VOD_CDN_DOMAIN,
    vodTemplateGroupId: env.ALIBABA_VOD_TEMPLATE_GROUP_ID,
    vodCallbackAuthKey: env.ALIBABA_VOD_CALLBACK_AUTH_KEY,
  },
  feedSignedUrlTtlSeconds: env.FEED_SIGNED_URL_TTL_SECONDS,
  upload: {
    signedUrlTtlSeconds: env.UPLOAD_SIGNED_URL_TTL_SECONDS,
    maxFileSizeBytes: env.UPLOAD_MAX_FILE_SIZE_BYTES,
    maxDurationMs: env.UPLOAD_MAX_DURATION_MS,
    maxConcurrentUploadsPerUser: env.UPLOAD_MAX_CONCURRENT_PER_USER,
  },
} as const;
