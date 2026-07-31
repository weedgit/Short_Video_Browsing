import express from "express";
import request from "supertest";
import { describe, expect, it } from "vitest";
import { createAuthRateLimiter } from "../middleware/createAuthRateLimiter";

describe("Auth rate limit (TEST-016)", () => {
  it("returns 429 after exceeding the auth rate limit", async () => {
    const app = express();
    app.use(createAuthRateLimiter(2));
    app.post("/login", (_req, res) => {
      res.status(200).json({ ok: true });
    });

    await request(app).post("/login").expect(200);
    await request(app).post("/login").expect(200);

    const limited = await request(app).post("/login");
    expect(limited.status).toBe(429);
    expect(limited.body.error.code).toBe("RATE_LIMITED");
  });
});
