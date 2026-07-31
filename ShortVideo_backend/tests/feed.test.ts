import { execSync } from "node:child_process";
import request from "supertest";
import { afterAll, beforeAll, beforeEach, describe, expect, it } from "vitest";
import { createApp } from "../app";
import { getPrismaClient } from "../db/client";
import { canConnectToDatabase, resetAuthTestData, resetFeedTestData } from "./helpers/test-db";

const dbReady = await canConnectToDatabase();

if (dbReady) {
  execSync("npx prisma db push --schema=db/prisma/schema.prisma --skip-generate", {
    stdio: "pipe",
  });
}

const describeFeed = dbReady ? describe : describe.skip;

describeFeed("Feed endpoints", () => {
  const appPromise = createApp();
  const deviceId = `test-device-${Date.now()}`;

  beforeAll(async () => {
    await resetAuthTestData();
  });

  beforeEach(async () => {
    await resetFeedTestData();
    await seedVideosForFeedTests();
  });

  afterAll(async () => {
    await resetAuthTestData();
  });

  it("GET /v1/feed returns cursor-paginated items (TEST-014 setup)", async () => {
    const app = await appPromise;
    const firstPage = await request(app)
      .get("/v1/feed?limit=5")
      .set("X-Device-Id", deviceId);

    expect(firstPage.status).toBe(200);
    expect(firstPage.body.data.items).toHaveLength(5);
    expect(firstPage.body.data.hasMore).toBe(true);
    expect(firstPage.body.data.nextCursor).toBeTypeOf("string");

    const secondPage = await request(app)
      .get(`/v1/feed?limit=5&cursor=${encodeURIComponent(firstPage.body.data.nextCursor)}`)
      .set("X-Device-Id", deviceId);

    expect(secondPage.status).toBe(200);
    expect(secondPage.body.data.items).toHaveLength(5);

    const firstIds = firstPage.body.data.items.map((item: { id: string }) => item.id);
    const secondIds = secondPage.body.data.items.map((item: { id: string }) => item.id);
    const overlap = firstIds.filter((id: string) => secondIds.includes(id));
    expect(overlap).toHaveLength(0);
  });

  it("GET /v1/feed avoids duplicate impressions for the same audience (API-013 / TEST-014)", async () => {
    const app = await appPromise;
    const audienceDevice = `${deviceId}-dedupe`;

    const pageOne = await request(app)
      .get("/v1/feed?limit=8")
      .set("X-Device-Id", audienceDevice);
    const pageTwo = await request(app)
      .get("/v1/feed?limit=8")
      .set("X-Device-Id", audienceDevice);

    expect(pageOne.status).toBe(200);
    expect(pageTwo.status).toBe(200);

    const seen = new Set<string>();
    for (const item of [...pageOne.body.data.items, ...pageTwo.body.data.items]) {
      expect(seen.has(item.id)).toBe(false);
      seen.add(item.id);
    }
  });

  it("GET /v1/feed seeds demo videos when database has no videos", async () => {
    const prisma = getPrismaClient();
    await resetFeedTestData();
    await prisma.user.deleteMany();

    const app = await appPromise;
    const response = await request(app)
      .get("/v1/feed?limit=5")
      .set("X-Device-Id", `${deviceId}-seed`);

    expect(response.status).toBe(200);
    expect(response.body.data.items.length).toBeGreaterThan(0);
  });

  it("POST /v1/playback/events/batch accepts playback events", async () => {
    const app = await appPromise;
    const feedResponse = await request(app)
      .get("/v1/feed?limit=1")
      .set("X-Device-Id", deviceId);

    const videoId = feedResponse.body.data.items[0].id as string;

    const response = await request(app)
      .post("/v1/playback/events/batch")
      .set("X-Device-Id", deviceId)
      .send({
        events: [
          {
            videoId,
            eventType: "PLAY",
            positionMs: 0,
          },
          {
            videoId,
            eventType: "PROGRESS",
            positionMs: 1500,
          },
        ],
      });

    expect(response.status).toBe(202);
    expect(response.body.data.accepted).toBe(2);
  });
});

async function seedVideosForFeedTests(): Promise<void> {
  const prisma = getPrismaClient();

  const owner = await prisma.user.create({
    data: {
      email: `feed-owner-${Date.now()}@example.com`,
      passwordHash: "hash",
      username: `feed_owner_${Date.now()}`,
      displayName: "Feed Owner",
    },
  });

  for (let index = 0; index < 12; index += 1) {
    await prisma.video.create({
      data: {
        userId: owner.id,
        description: `Feed test video ${index + 1}`,
        durationMs: 10_000 + index * 1_000,
        status: "READY",
        streamUrl: "https://download.samplelib.com/mp4/sample-10s.mp4",
        category: "Test",
        hashtags: {
          create: [{ tag: "#test" }],
        },
        createdAt: new Date(Date.now() - index * 60_000),
      },
    });
  }
}
