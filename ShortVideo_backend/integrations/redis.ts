import { createClient, type RedisClientType } from "redis";
import { config } from "../config";
import { logger } from "../utils/logger";

let redisClient: RedisClientType | null = null;
let connectPromise: Promise<RedisClientType | null> | null = null;

export async function getRedisClient(): Promise<RedisClientType | null> {
  if (!config.redisUrl) {
    return null;
  }

  if (redisClient?.isOpen) {
    return redisClient;
  }

  if (!connectPromise) {
    connectPromise = (async () => {
      try {
        const client = createClient({ url: config.redisUrl });
        client.on("error", (error) => {
          logger.warn({ err: error }, "Redis client error");
        });
        await client.connect();
        redisClient = client as RedisClientType;
        return redisClient;
      } catch (error) {
        logger.warn({ err: error }, "Redis unavailable, falling back to in-memory stores");
        return null;
      }
    })();
  }

  return connectPromise;
}

export async function disconnectRedis(): Promise<void> {
  if (redisClient?.isOpen) {
    await redisClient.quit();
  }
  redisClient = null;
  connectPromise = null;
}
