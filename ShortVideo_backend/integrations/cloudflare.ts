import { config } from "../config";
import type { SignedPlaybackUrl } from "../models/feed.types";
import type { DirectUploadResult } from "../models/upload.types";
import { logger } from "../utils/logger";

type PlaybackSource = {
  cloudflareAssetId?: string | null;
  hlsUrl?: string | null;
  streamUrl?: string | null;
};

export function isCloudflareConfigured(): boolean {
  return Boolean(
    config.cloudflare.accountId &&
      config.cloudflare.apiToken &&
      config.cloudflare.streamCustomerSubdomain,
  );
}

export async function createDirectUpload(params: {
  maxDurationSeconds: number;
  fileSizeBytes: number;
}): Promise<DirectUploadResult> {
  const expiresAt = new Date(Date.now() + config.upload.signedUrlTtlSeconds * 1000);

  if (!isCloudflareConfigured()) {
    const assetId = `dev-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
    return {
      uploadUrl: `https://upload.cloudflarestream.com/dev/${assetId}`,
      cloudflareAssetId: assetId,
      expiresAt,
    };
  }

  const response = await fetch(
    `https://api.cloudflare.com/client/v4/accounts/${config.cloudflare.accountId}/stream/direct_upload`,
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${config.cloudflare.apiToken}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        maxDurationSeconds: params.maxDurationSeconds,
        meta: {
          fileSizeBytes: params.fileSizeBytes,
        },
      }),
    },
  );

  if (!response.ok) {
    logger.error({ status: response.status }, "Cloudflare direct upload creation failed");
    throw new Error("Failed to create Cloudflare direct upload");
  }

  const payload = (await response.json()) as {
    result?: { uploadURL?: string; uid?: string };
  };

  if (!payload.result?.uploadURL || !payload.result.uid) {
    throw new Error("Invalid Cloudflare direct upload response");
  }

  return {
    uploadUrl: payload.result.uploadURL,
    cloudflareAssetId: payload.result.uid,
    expiresAt,
  };
}

export function buildAssetPlaybackUrls(assetId: string): { hlsUrl: string; streamUrl: string } {
  const subdomain = config.cloudflare.streamCustomerSubdomain ?? "videodelivery.net";
  return {
    hlsUrl: `https://${subdomain}/${assetId}/manifest/video.m3u8`,
    streamUrl: `https://${subdomain}/${assetId}/downloads/default.mp4`,
  };
}

export function buildAssetThumbnailUrl(assetId: string): string {
  const subdomain = config.cloudflare.streamCustomerSubdomain ?? "videodelivery.net";
  return `https://${subdomain}/${assetId}/thumbnails/thumbnail.jpg`;
}

export function resolveThumbnailUrl(source: {
  thumbnailUrl?: string | null;
  cloudflareAssetId?: string | null;
}): string | null {
  if (source.thumbnailUrl) return source.thumbnailUrl;
  if (source.cloudflareAssetId) return buildAssetThumbnailUrl(source.cloudflareAssetId);
  return null;
}

export function resolveSignedPlaybackUrl(source: PlaybackSource): SignedPlaybackUrl {
  const expiresAt = new Date(Date.now() + config.feedSignedUrlTtlSeconds * 1000);

  if (source.hlsUrl && isCloudflareConfigured() && source.cloudflareAssetId) {
    const signedUrl = signCloudflareStreamUrl(source.cloudflareAssetId, expiresAt);
    return {
      url: signedUrl,
      expiresAt,
      format: "hls",
    };
  }

  const directUrl = source.hlsUrl ?? source.streamUrl;
  if (!directUrl) {
    throw new Error("Video has no playback URL configured");
  }

  return {
    url: directUrl,
    expiresAt,
    format: source.hlsUrl ? "hls" : "mp4",
  };
}

function signCloudflareStreamUrl(assetId: string, expiresAt: Date): string {
  const subdomain = config.cloudflare.streamCustomerSubdomain!;
  const exp = Math.floor(expiresAt.getTime() / 1000);
  const baseUrl = `https://${subdomain}/${assetId}/manifest/video.m3u8`;

  if (!config.cloudflare.apiToken) {
    return baseUrl;
  }

  try {
    // Cloudflare Stream signed URL token (simplified dev signing using query token).
    const tokenPayload = `${assetId}:${exp}`;
    const token = Buffer.from(tokenPayload).toString("base64url");
    return `${baseUrl}?token=${token}`;
  } catch (error) {
    logger.warn({ error, assetId }, "Failed to sign Cloudflare URL, using unsigned HLS URL");
    return baseUrl;
  }
}
