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

if (dbReady) {
  execSync("npx prisma db push --schema=db/prisma/schema.prisma --skip-generate", {
    stdio: "pipe",
  });
}

const describeSecurity = dbReady ? describe : describe.skip;

describeSecurity("Auth security (TEST-018)", () => {
  const appPromise = createApp();
  const testUser = {
    email: `security-${Date.now()}@example.com`,
    password: "password123",
    username: `sec_${Date.now()}`,
    displayName: "Security Test",
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

  it("DELETE /v1/account without token returns 401", async () => {
    const app = await appPromise;
    const response = await request(app).delete("/v1/account");

    expect(response.status).toBe(401);
    expect(response.body.error.code).toBe("UNAUTHORIZED");
  });

  it("DELETE /v1/account with invalid token returns 401", async () => {
    const app = await appPromise;
    const response = await request(app)
      .delete("/v1/account")
      .set("Authorization", "Bearer invalid.token.value");

    expect(response.status).toBe(401);
    expect(response.body.error.code).toBe("UNAUTHORIZED");
  });

  it("DELETE /v1/account only deletes the authenticated user", async () => {
    const app = await appPromise;
    const suffix = Date.now();

    const victim = await request(app).post("/v1/auth/register").send({
      ...testUser,
      email: `victim-${suffix}@example.com`,
      username: `victim_${suffix}`,
    });
    const attacker = await request(app).post("/v1/auth/register").send({
      ...testUser,
      email: `attacker-${suffix}@example.com`,
      username: `attacker_${suffix}`,
    });

    const attackerToken = attacker.body.data.tokens.accessToken as string;
    const victimUserId = victim.body.data.user.id as string;

    const deleteResponse = await request(app)
      .delete("/v1/account")
      .set("Authorization", `Bearer ${attackerToken}`);

    expect(deleteResponse.status).toBe(200);

    const victimLogin = await request(app).post("/v1/auth/login").send({
      email: `victim-${suffix}@example.com`,
      password: testUser.password,
    });
    expect(victimLogin.status).toBe(200);
    expect(victimLogin.body.data.user.id).toBe(victimUserId);

    const attackerLogin = await request(app).post("/v1/auth/login").send({
      email: `attacker-${suffix}@example.com`,
      password: testUser.password,
    });
    expect(attackerLogin.status).toBe(403);
  });
});
