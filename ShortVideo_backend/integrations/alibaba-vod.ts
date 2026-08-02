import Vod20170321, {
  CreateUploadVideoRequest,
  GetPlayInfoRequest,
} from "@alicloud/vod20170321";
import { Config } from "@alicloud/openapi-client";
import { config } from "../config";
import type { SignedPlaybackUrl } from "../models/feed.types";
import type { DirectUploadResult } from "../models/upload.types";
import { logger } from "../utils/logger";
import { vodExtensionFromMime } from "../utils/vod-video-formats";

type PlaybackSource = {
  /** Alibaba VOD VideoId (DB column: cloudflare_asset_id). */
  cloudflareAssetId?: string | null;
  hlsUrl?: string | null;
  streamUrl?: string | null;
};

export function isAlibabaVodConfigured(): boolean {
  return Boolean(
    config.alibaba.accessKeyId &&
      config.alibaba.accessKeySecret &&
      config.alibaba.vodRegionId,
  );
}

/** @deprecated Cloudflare removed — alias for isAlibabaVodConfigured. */
export function isCloudflareConfigured(): boolean {
  return isAlibabaVodConfigured();
}

function createVodClient(): Vod20170321 {
  const openApiConfig = new Config({
    accessKeyId: config.alibaba.accessKeyId,
    accessKeySecret: config.alibaba.accessKeySecret,
    endpoint: `vod.${config.alibaba.vodRegionId}.aliyuncs.com`,
  });
  return new Vod20170321(openApiConfig);
}

export async function createDirectUpload(params: {
  maxDurationSeconds: number;
  fileSizeBytes: number;
  mimeType?: string;
  title?: string;
}): Promise<DirectUploadResult> {
  const expiresAt = new Date(Date.now() + config.upload.signedUrlTtlSeconds * 1000);
  const ext = vodExtensionFromMime(params.mimeType ?? "video/mp4");
  const fileName = `upload-${Date.now()}.${ext}`;
  const title = params.title ?? fileName;

  if (!isAlibabaVodConfigured()) {
    const assetId = `dev-ali-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
    return {
      provider: "dev",
      uploadUrl: `https://vod.aliyuncs.com/dev/${assetId}`,
      cloudflareAssetId: assetId,
      expiresAt,
      uploadAuth: null,
      uploadAddress: null,
    };
  }

  const client = createVodClient();
  const request = new CreateUploadVideoRequest({
    title,
    fileName,
    fileSize: params.fileSizeBytes,
    templateGroupId: config.alibaba.vodTemplateGroupId || undefined,
  });

  try {
    const response = await client.createUploadVideo(request);
    const body = response.body;
    if (!body?.videoId || !body.uploadAuth || !body.uploadAddress) {
      throw new Error("Invalid CreateUploadVideo response");
    }

    const addressJson = JSON.parse(
      Buffer.from(body.uploadAddress, "base64").toString("utf8"),
    ) as { Endpoint?: string; Bucket?: string; FileName?: string };

    const endpoint = (addressJson.Endpoint ?? "").replace(/^https?:\/\//, "");
    const bucket = addressJson.Bucket ?? "";
    const objectKey = addressJson.FileName ?? fileName;
    const uploadUrl = `https://${bucket}.${endpoint}/${objectKey}`;

    return {
      provider: "alibaba_vod",
      uploadUrl,
      cloudflareAssetId: body.videoId,
      expiresAt,
      uploadAuth: body.uploadAuth,
      uploadAddress: body.uploadAddress,
    };
  } catch (error) {
    logger.error({ error }, "Alibaba VOD CreateUploadVideo failed");
    throw new Error("Failed to create Alibaba VOD upload");
  }
}

export async function fetchPlaybackUrls(videoId: string): Promise<{
  hlsUrl: string | null;
  streamUrl: string | null;
  coverUrl: string | null;
}> {
  if (!isAlibabaVodConfigured()) {
    const urls = buildAssetPlaybackUrls(videoId);
    return { ...urls, coverUrl: null };
  }

  try {
    const client = createVodClient();
    const request = new GetPlayInfoRequest({ videoId });
    const response = await client.getPlayInfo(request);
    const playList = response.body?.playInfoList?.playInfo ?? [];
    const coverUrl = response.body?.videoBase?.coverURL ?? null;

    const hls =
      playList.find((p) => (p.format ?? "").toLowerCase() === "m3u8")?.playURL ??
      playList.find((p) => (p.playURL ?? "").includes(".m3u8"))?.playURL ??
      null;
    const mp4 =
      playList.find((p) => (p.format ?? "").toLowerCase() === "mp4")?.playURL ??
      playList.find((p) => (p.playURL ?? "").includes(".mp4"))?.playURL ??
      playList[0]?.playURL ??
      null;

    const rewrite = (url: string | null | undefined): string | null => {
      if (!url) return null;
      const cdnHost = config.alibaba.vodCdnDomain
        ?.replace(/^https?:\/\//, "")
        .replace(/\/$/, "")
        .trim();
      if (!cdnHost) return url;
      try {
        const parsed = new URL(url);
        parsed.host = cdnHost;
        return parsed.toString();
      } catch {
        return url;
      }
    };

    return {
      hlsUrl: rewrite(hls) ?? rewrite(mp4),
      streamUrl: rewrite(mp4) ?? rewrite(hls),
      coverUrl,
    };
  } catch (error) {
    logger.warn({ error, videoId }, "GetPlayInfo failed; falling back to CDN template URLs");
    const urls = buildAssetPlaybackUrls(videoId);
    return { ...urls, coverUrl: null };
  }
}

export function buildAssetPlaybackUrls(assetId: string): { hlsUrl: string; streamUrl: string } {
  const domain =
    config.alibaba.vodCdnDomain?.replace(/^https?:\/\//, "").replace(/\/$/, "") ||
    `vod.${config.alibaba.vodRegionId || "cn-shanghai"}.aliyuncs.com`;

  return {
    hlsUrl: `https://${domain}/${assetId}/index.m3u8`,
    streamUrl: `https://${domain}/${assetId}/index.mp4`,
  };
}

export function buildAssetThumbnailUrl(assetId: string): string {
  const domain =
    config.alibaba.vodCdnDomain?.replace(/^https?:\/\//, "").replace(/\/$/, "") ||
    `vod.${config.alibaba.vodRegionId || "cn-shanghai"}.aliyuncs.com`;
  return `https://${domain}/${assetId}/cover.jpg`;
}

export function resolveThumbnailUrl(source: {
  thumbnailUrl?: string | null;
  cloudflareAssetId?: string | null;
}): string | null {
  if (source.thumbnailUrl) return source.thumbnailUrl;
  if (source.cloudflareAssetId) return buildAssetThumbnailUrl(source.cloudflareAssetId);
  return null;
}

function isPlayableHttpUrl(url: string): boolean {
  try {
    const parsed = new URL(url);
    return (
      (parsed.protocol === "https:" || parsed.protocol === "http:") &&
      Boolean(parsed.hostname)
    );
  } catch {
    return false;
  }
}

export function resolveSignedPlaybackUrl(source: PlaybackSource): SignedPlaybackUrl {
  const expiresAt = new Date(Date.now() + config.feedSignedUrlTtlSeconds * 1000);
  const candidates = [source.hlsUrl, source.streamUrl].filter(
    (value): value is string => Boolean(value && isPlayableHttpUrl(value)),
  );
  const directUrl = candidates[0];
  if (!directUrl) {
    throw new Error("Video has no playback URL configured");
  }

  return {
    url: directUrl,
    expiresAt,
    format: directUrl.includes(".m3u8") ? "hls" : "mp4",
  };
}
