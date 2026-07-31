import type { FeedCursorPayload } from "../models/feed.types";

export function encodeFeedCursor(payload: FeedCursorPayload): string {
  return Buffer.from(JSON.stringify(payload), "utf8").toString("base64url");
}

export function decodeFeedCursor(cursor: string): FeedCursorPayload {
  const decoded = Buffer.from(cursor, "base64url").toString("utf8");
  const parsed = JSON.parse(decoded) as FeedCursorPayload;

  if (!parsed.createdAt || !parsed.id) {
    throw new Error("Invalid feed cursor");
  }

  return parsed;
}

export function formatUploadedAtLabel(createdAt: Date): string {
  const diffMs = Date.now() - createdAt.getTime();
  const minutes = Math.floor(diffMs / 60_000);
  if (minutes < 1) return "Just now";
  if (minutes < 60) return `${minutes} min ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours} hour${hours === 1 ? "" : "s"} ago`;
  const days = Math.floor(hours / 24);
  return `${days} day${days === 1 ? "" : "s"} ago`;
}
