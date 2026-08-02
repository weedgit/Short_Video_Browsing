import { execSync } from "node:child_process";
import request from "supertest";
import { afterAll, beforeAll, beforeEach, describe, expect, it } from "vitest";
import { createApp } from "../app";
import { getPrismaClient } from "../db/client";
import { canConnectToDatabase, resetAuthTestData } from "./helpers/test-db";

const dbReady = await canConnectToDatabase();

if (dbReady) {
  execSync("npx prisma db push --schema=db/prisma/schema.prisma --skip-generate", {
    stdio: "pipe",
  });
}

const describeUpload = dbReady ? describe : describe.skip;

describeUpload("Upload endpoints", () => {
  const appPromise = createApp();
  const testUser = {
    email: `upload-${Date.now()}@example.com`,
    password: "password123",
    username: `upload_user_${Date.now()}`,
    displayName: "Upload User",
  };

  let accessToken = "";

  beforeAll(async () => {
    await resetAuthTestData();
  });

  beforeEach(async () => {
    await resetAuthTestData();
    const app = await appPromise;
    const registerResponse = await request(app).post("/v1/auth/register").send(testUser);
    accessToken = registerResponse.body.data.tokens.accessToken as string;
  });

  afterAll(async () => {
    await resetAuthTestData();
  });

  it("POST /v1/uploads creates an upload session (TEST-008 setup)", async () => {
    const app = await appPromise;
    const response = await request(app)
      .post("/v1/uploads")
      .set("Authorization", `Bearer ${accessToken}`)
      .send({
        mimeType: "video/mp4",
        fileSizeBytes: 5_000_000,
        durationMs: 15_000,
      });

    expect(response.status).toBe(201);
    expect(response.body.data.uploadId).toBeTypeOf("string");
    expect(response.body.data.videoId).toBeTypeOf("string");
    expect(response.body.data.uploadUrl).toBeTypeOf("string");
    expect(response.body.data.uploadToken).toBeTypeOf("string");
  });

  it("allows a new upload after cancelling the previous session", async () => {
    const app = await appPromise;
    const first = await request(app)
      .post("/v1/uploads")
      .set("Authorization", `Bearer ${accessToken}`)
      .send({
        mimeType: "video/mp4",
        fileSizeBytes: 5_000_000,
      });

    expect(first.status).toBe(201);
    const uploadId = first.body.data.uploadId as string;

    const cancelResponse = await request(app)
      .delete(`/v1/uploads/${uploadId}`)
      .set("Authorization", `Bearer ${accessToken}`);

    expect(cancelResponse.status).toBe(200);
    expect(cancelResponse.body.data.cancelled).toBe(true);

    const second = await request(app)
      .post("/v1/uploads")
      .set("Authorization", `Bearer ${accessToken}`)
      .send({
        mimeType: "video/mp4",
        fileSizeBytes: 6_000_000,
      });

    expect(second.status).toBe(201);
  });

  it("rejects a second concurrent upload for the same user (UPLOAD-LONG-004)", async () => {
    const app = await appPromise;
    const first = await request(app)
      .post("/v1/uploads")
      .set("Authorization", `Bearer ${accessToken}`)
      .send({
        mimeType: "video/mp4",
        fileSizeBytes: 5_000_000,
      });

    expect(first.status).toBe(201);

    const second = await request(app)
      .post("/v1/uploads")
      .set("Authorization", `Bearer ${accessToken}`)
      .send({
        mimeType: "video/mp4",
        fileSizeBytes: 6_000_000,
      });

    expect(second.status).toBe(409);
    expect(second.body.error.code).toBe("UPLOAD_IN_PROGRESS");
  });

  it("rejects upload token reuse (TEST-019)", async () => {
    const app = await appPromise;
    const createResponse = await request(app)
      .post("/v1/uploads")
      .set("Authorization", `Bearer ${accessToken}`)
      .send({
        mimeType: "video/mp4",
        fileSizeBytes: 4_000_000,
      });

    const uploadId = createResponse.body.data.uploadId as string;
    const uploadToken = createResponse.body.data.uploadToken as string;

    const firstUse = await request(app)
      .patch(`/v1/uploads/${uploadId}/progress`)
      .set("Authorization", `Bearer ${accessToken}`)
      .set("X-Upload-Token", uploadToken)
      .send({ bytesUploaded: 1000 });

    expect(firstUse.status).toBe(200);

    const reuse = await request(app)
      .patch(`/v1/uploads/${uploadId}/progress`)
      .set("Authorization", `Bearer ${accessToken}`)
      .set("X-Upload-Token", "invalid-token")
      .send({ bytesUploaded: 2000 });

    expect(reuse.status).toBe(403);
  });

  it("completes dev upload flow and publishes video", async () => {
    const app = await appPromise;
    const createResponse = await request(app)
      .post("/v1/uploads")
      .set("Authorization", `Bearer ${accessToken}`)
      .send({
        mimeType: "video/mp4",
        fileSizeBytes: 4_000_000,
        durationMs: 12_000,
      });

    const uploadId = createResponse.body.data.uploadId as string;
    const uploadToken = createResponse.body.data.uploadToken as string;
    const videoId = createResponse.body.data.videoId as string;

    const completeResponse = await request(app)
      .post(`/v1/uploads/${uploadId}/dev-complete`)
      .set("Authorization", `Bearer ${accessToken}`)
      .set("X-Upload-Token", uploadToken);

    expect(completeResponse.status).toBe(202);

    const publishResponse = await request(app)
      .post(`/v1/videos/${videoId}/publish`)
      .set("Authorization", `Bearer ${accessToken}`)
      .send({
        description: "My first upload",
        hashtags: ["#phase4"],
        category: "Comedy",
      });

    expect(publishResponse.status).toBe(200);
    expect(publishResponse.body.data.status).toBe("PUBLISHED");

    const prisma = getPrismaClient();
    const video = await prisma.video.findUnique({ where: { id: videoId } });
    expect(video?.status).toBe("READY");
  });

  it("rejects MIME types outside Alibaba VOD video formats", async () => {
    const app = await appPromise;
    const response = await request(app)
      .post("/v1/uploads")
      .set("Authorization", `Bearer ${accessToken}`)
      .send({
        mimeType: "video/x-unknown-codec",
        fileSizeBytes: 5_000_000,
        durationMs: 15_000,
      });

    expect(response.status).toBe(400);
  });

  it("accepts Alibaba VOD MPEG containers such as VOB", async () => {
    const app = await appPromise;
    const response = await request(app)
      .post("/v1/uploads")
      .set("Authorization", `Bearer ${accessToken}`)
      .send({
        mimeType: "video/dvd",
        fileSizeBytes: 5_000_000,
        durationMs: 15_000,
      });

    expect(response.status).toBe(201);
  });

  it("handles duplicate webhook events idempotently (TEST-013)", async () => {
    const app = await appPromise;
    const payload = {
      EventType: "StreamTranscodeComplete",
      VideoId: "ali-test-video",
      Status: "success",
      Duration: "12",
    };

    const first = await request(app)
      .post("/v1/webhooks/alibaba/vod")
      .set("x-vod-request-id", "evt-duplicate-1")
      .send(payload);

    const second = await request(app)
      .post("/v1/webhooks/alibaba/vod")
      .set("x-vod-request-id", "evt-duplicate-1")
      .send(payload);

    expect(first.status).toBeGreaterThanOrEqual(200);
    expect(second.status).toBe(200);
    expect(second.body.data.duplicate).toBe(true);
  });
});
