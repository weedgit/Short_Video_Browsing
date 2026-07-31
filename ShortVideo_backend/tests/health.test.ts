import request from "supertest";
import { describe, expect, it } from "vitest";
import { createApp } from "../app";

describe("Health endpoints", () => {
  const appPromise = createApp();

  it("GET /health/live returns 200", async () => {
    const app = await appPromise;
    const response = await request(app).get("/health/live");

    expect(response.status).toBe(200);
    expect(response.body.status).toBe("ok");
    expect(response.headers["x-request-id"]).toBeDefined();
  });

  it("GET /health/ready returns readiness payload", async () => {
    const app = await appPromise;
    const response = await request(app).get("/health/ready");

    expect([200, 503]).toContain(response.status);
    expect(response.body.checks).toBeDefined();
  });

  it("GET /v1 returns API info", async () => {
    const app = await appPromise;
    const response = await request(app).get("/v1");

    expect(response.status).toBe(200);
    expect(response.body.apiVersion).toBe("v1");
  });

  it("GET /unknown returns 404 with error envelope", async () => {
    const app = await appPromise;
    const response = await request(app).get("/unknown");

    expect(response.status).toBe(404);
    expect(response.body.error.code).toBe("NOT_FOUND");
  });

  it("GET /openapi.json returns OpenAPI spec", async () => {
    const app = await appPromise;
    const response = await request(app).get("/openapi.json");

    expect(response.status).toBe(200);
    expect(response.body.openapi).toBe("3.0.3");
    expect(response.body.paths["/v1/auth/login"]).toBeDefined();
  });
});
