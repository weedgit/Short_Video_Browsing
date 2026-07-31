import rateLimit, { type RateLimitRequestHandler } from "express-rate-limit";
import { RedisStore, type RedisReply } from "rate-limit-redis";
import { getRedisClient } from "../integrations/redis";

const AUTH_RATE_LIMIT_WINDOW_MS = 15 * 60 * 1000;
const AUTH_RATE_LIMIT_MAX = 30;

const limiterOptions = {
  windowMs: AUTH_RATE_LIMIT_WINDOW_MS,
  max: AUTH_RATE_LIMIT_MAX,
  standardHeaders: true,
  legacyHeaders: false,
  message: {
    error: {
      code: "RATE_LIMITED",
      message: "Too many authentication requests. Please try again later.",
    },
  },
};

export function createAuthRateLimiter(maxRequests = AUTH_RATE_LIMIT_MAX): RateLimitRequestHandler {
  return rateLimit({
    ...limiterOptions,
    max: maxRequests,
  });
}

export async function createAuthRateLimiterWithStore(
  maxRequests = AUTH_RATE_LIMIT_MAX,
): Promise<RateLimitRequestHandler> {
  const redisClient = await getRedisClient();

  if (!redisClient) {
    return createAuthRateLimiter(maxRequests);
  }

  return rateLimit({
    ...limiterOptions,
    max: maxRequests,
    store: new RedisStore({
      sendCommand: (...args: string[]) =>
        redisClient.sendCommand(args) as Promise<RedisReply>,
      prefix: "auth-rate-limit:",
    }),
  });
}

export const AUTH_RATE_LIMIT_MAX_REQUESTS = AUTH_RATE_LIMIT_MAX;
