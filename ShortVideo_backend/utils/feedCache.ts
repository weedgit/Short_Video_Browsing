import { getRedisClient } from "../integrations/redis";
import type { FeedPage } from "../models/feed.types";
import { logger } from "./logger";

const FOR_YOU_FIRST_PAGE_TTL_SECONDS = 30;

function buildCacheKey(audienceKey: string): string {
  return `feed:foryou:first-page:${audienceKey}`;
}

export async function getCachedForYouFirstPage(audienceKey: string): Promise<FeedPage | null> {
  try {
    const client = await getRedisClient();
    if (!client) return null;

    const raw = await client.get(buildCacheKey(audienceKey));
    if (!raw) return null;

    return JSON.parse(raw) as FeedPage;
  } catch (error) {
    logger.warn({ error }, "Failed to read feed cache, falling back to database");
    return null;
  }
}

export async function setCachedForYouFirstPage(audienceKey: string, page: FeedPage): Promise<void> {
  try {
    const client = await getRedisClient();
    if (!client) return;

    await client.set(buildCacheKey(audienceKey), JSON.stringify(page), {
      EX: FOR_YOU_FIRST_PAGE_TTL_SECONDS,
    });
  } catch (error) {
    logger.warn({ error }, "Failed to write feed cache");
  }
}
