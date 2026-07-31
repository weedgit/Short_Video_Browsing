import { createHash, randomBytes } from "node:crypto";

export function generateUploadToken(): string {
  return randomBytes(32).toString("base64url");
}

export function hashUploadToken(token: string): string {
  return createHash("sha256").update(token).digest("hex");
}

export function hashPayload(payload: string): string {
  return createHash("sha256").update(payload).digest("hex");
}
