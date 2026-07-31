import { execSync } from "node:child_process";
import request from "supertest";
import { afterAll, beforeAll, beforeEach, describe, expect, it } from "vitest";
import { createApp } from "../app";
import { canConnectToDatabase, resetAuthTestData } from "./helpers/test-db";

const dbReady = await canConnectToDatabase();

if (dbReady) {
  execSync("npx prisma db push --schema=db/prisma/schema.prisma --skip-generate", {
    stdio: "pipe",
  });
}

const describeAuth = dbReady ? describe : describe.skip;

describeAuth("Auth endpoints", () => {
  const appPromise = createApp();
  const testUser = {
    email: `test-${Date.now()}@example.com`,
    password: "password123",
    username: `user_${Date.now()}`,
    displayName: "Test User",
  };

  beforeAll(async () => {
    await resetAuthTestData();
  });

  beforeEach(async () => {
    await resetAuthTestData();
  });

  afterAll(async () => {
    await resetAuthTestData();
  });

  it("POST /v1/auth/register creates a user and returns tokens", async () => {
    const app = await appPromise;
    const response = await request(app).post("/v1/auth/register").send(testUser);

    expect(response.status).toBe(201);
    expect(response.body.data.user.email).toBe(testUser.email.toLowerCase());
    expect(response.body.data.tokens.accessToken).toBeTypeOf("string");
    expect(response.body.data.tokens.refreshToken).toBeTypeOf("string");
  });

  it("POST /v1/auth/login returns tokens for valid credentials", async () => {
    const app = await appPromise;
    await request(app).post("/v1/auth/register").send(testUser);

    const response = await request(app).post("/v1/auth/login").send({
      email: testUser.email,
      password: testUser.password,
    });

    expect(response.status).toBe(200);
    expect(response.body.data.tokens.tokenType).toBe("Bearer");
  });

  it("POST /v1/auth/refresh rotates refresh token", async () => {
    const app = await appPromise;
    const registerResponse = await request(app).post("/v1/auth/register").send(testUser);
    const refreshToken = registerResponse.body.data.tokens.refreshToken as string;

    const response = await request(app).post("/v1/auth/refresh").send({ refreshToken });

    expect(response.status).toBe(200);
    expect(response.body.data.tokens.refreshToken).not.toBe(refreshToken);
  });

  it("POST /v1/auth/logout revokes refresh token", async () => {
    const app = await appPromise;
    const registerResponse = await request(app).post("/v1/auth/register").send(testUser);
    const refreshToken = registerResponse.body.data.tokens.refreshToken as string;

    const logoutResponse = await request(app).post("/v1/auth/logout").send({ refreshToken });
    expect(logoutResponse.status).toBe(200);

    const refreshResponse = await request(app).post("/v1/auth/refresh").send({ refreshToken });
    expect(refreshResponse.status).toBe(401);
    expect(refreshResponse.body.error.code).toBe("INVALID_REFRESH_TOKEN");
  });

  it("POST /v1/auth/password/reset/request and confirm updates password", async () => {
    const app = await appPromise;
    await request(app).post("/v1/auth/register").send(testUser);

    const resetRequest = await request(app)
      .post("/v1/auth/password/reset/request")
      .send({ email: testUser.email });

    expect(resetRequest.status).toBe(200);
    expect(resetRequest.body.data.resetToken).toBeTypeOf("string");

    const resetToken = resetRequest.body.data.resetToken as string;
    const newPassword = "newpassword123";

    const confirmResponse = await request(app)
      .post("/v1/auth/password/reset/confirm")
      .send({ token: resetToken, newPassword });

    expect(confirmResponse.status).toBe(200);

    const loginResponse = await request(app).post("/v1/auth/login").send({
      email: testUser.email,
      password: newPassword,
    });

    expect(loginResponse.status).toBe(200);
  });

  it("DELETE /v1/account soft-deletes the authenticated user", async () => {
    const app = await appPromise;
    const registerResponse = await request(app).post("/v1/auth/register").send(testUser);
    const accessToken = registerResponse.body.data.tokens.accessToken as string;

    const deleteResponse = await request(app)
      .delete("/v1/account")
      .set("Authorization", `Bearer ${accessToken}`);

    expect(deleteResponse.status).toBe(200);

    const loginResponse = await request(app).post("/v1/auth/login").send({
      email: testUser.email,
      password: testUser.password,
    });

    expect(loginResponse.status).toBe(403);
  });

  it("runs register -> login -> refresh -> logout E2E flow (TEST-012)", async () => {
    const app = await appPromise;
    const uniqueUser = {
      email: `e2e-${Date.now()}@example.com`,
      password: "password123",
      username: `e2e_${Date.now()}`,
      displayName: "E2E User",
    };

    const registerResponse = await request(app)
      .post("/v1/auth/register")
      .set("X-Device-Id", "device-e2e-001")
      .set("X-Platform", "android")
      .send(uniqueUser);
    expect(registerResponse.status).toBe(201);

    const loginResponse = await request(app).post("/v1/auth/login").send({
      email: uniqueUser.email,
      password: uniqueUser.password,
    });
    expect(loginResponse.status).toBe(200);

    let refreshToken = loginResponse.body.data.tokens.refreshToken as string;
    const refreshResponse = await request(app).post("/v1/auth/refresh").send({ refreshToken });
    expect(refreshResponse.status).toBe(200);
    refreshToken = refreshResponse.body.data.tokens.refreshToken as string;

    const logoutResponse = await request(app).post("/v1/auth/logout").send({ refreshToken });
    expect(logoutResponse.status).toBe(200);

    const refreshAfterLogout = await request(app).post("/v1/auth/refresh").send({ refreshToken });
    expect(refreshAfterLogout.status).toBe(401);
  });

  it("registers user device metadata on login", async () => {
    const app = await appPromise;
    const deviceUser = {
      email: `device-${Date.now()}@example.com`,
      password: "password123",
      username: `dev_${Date.now()}`,
      displayName: "Device User",
    };

    await request(app).post("/v1/auth/register").send(deviceUser);

    const loginResponse = await request(app)
      .post("/v1/auth/login")
      .set("X-Device-Id", "pixel-test-device")
      .set("X-Platform", "android")
      .set("X-App-Version", "0.1.0")
      .send({
        email: deviceUser.email,
        password: deviceUser.password,
      });

    expect(loginResponse.status).toBe(200);
  });
});
