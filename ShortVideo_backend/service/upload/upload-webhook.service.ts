import { completeUploadSession, recordWebhookEvent } from "../../db/repositories/upload.repository";
import {
  buildAssetPlaybackUrls,
  fetchPlaybackUrls,
} from "../../integrations/alibaba-vod";
import { enqueueUploadProcessingJob } from "../../jobs/queues";
import { hashPayload } from "../../utils/uploadToken";
import { getPrismaClient } from "../../db/client";
import { logger } from "../../utils/logger";

type AlibabaVodCallbackPayload = {
  EventType?: string;
  VideoId?: string;
  Status?: string;
  Duration?: string | number;
  CoverUrl?: string;
  StreamInfos?: Array<{
    FileUrl?: string;
    Format?: string;
    Height?: string | number;
  }>;
  // Legacy Cloudflare-shaped fields (ignored for Alibaba)
  uid?: string;
  status?: { state?: string };
};

export async function handleAlibabaVodWebhook(params: {
  eventId: string;
  payload: AlibabaVodCallbackPayload;
  rawBody: string;
}): Promise<{ duplicate: boolean; accepted: boolean }> {
  const payloadHash = hashPayload(params.rawBody);
  const inserted = await recordWebhookEvent({
    provider: "alibaba-vod",
    eventId: params.eventId,
    payloadHash,
  });

  if (!inserted) {
    return { duplicate: true, accepted: true };
  }

  const videoId = params.payload.VideoId ?? params.payload.uid;
  if (!videoId) {
    return { duplicate: false, accepted: false };
  }

  const eventType = (params.payload.EventType ?? "").toLowerCase();
  const status = (params.payload.Status ?? params.payload.status?.state ?? "").toLowerCase();

  // Prefer transcode-complete; also accept successful upload-complete when no transcoding.
  const isSuccess =
    status === "success" ||
    status === "ready" ||
    eventType.includes("streamtranscodecomplete") ||
    eventType.includes("transcodecomplete") ||
    eventType.includes("fileuploadcomplete");

  if (!isSuccess && status && status !== "success") {
    logger.info({ videoId, eventType, status }, "Ignoring non-success Alibaba VOD callback");
    return { duplicate: false, accepted: true };
  }

  const prisma = getPrismaClient();
  const session = await prisma.uploadSession.findFirst({
    where: { cloudflareAssetId: videoId },
  });

  if (!session) {
    return { duplicate: false, accepted: false };
  }

  let hlsUrl: string | null = null;
  let streamUrl: string | null = null;
  let coverUrl: string | null = params.payload.CoverUrl ?? null;

  const streams = params.payload.StreamInfos ?? [];
  hlsUrl =
    streams.find((s) => (s.Format ?? "").toLowerCase() === "m3u8")?.FileUrl ??
    streams.find((s) => (s.FileUrl ?? "").includes(".m3u8"))?.FileUrl ??
    null;
  streamUrl =
    streams.find((s) => (s.Format ?? "").toLowerCase() === "mp4")?.FileUrl ??
    streams.find((s) => (s.FileUrl ?? "").includes(".mp4"))?.FileUrl ??
    null;

  if (!hlsUrl && !streamUrl) {
    const fetched = await fetchPlaybackUrls(videoId);
    hlsUrl = fetched.hlsUrl;
    streamUrl = fetched.streamUrl;
    coverUrl = coverUrl ?? fetched.coverUrl;
  }

  if (!hlsUrl && !streamUrl) {
    const fallback = buildAssetPlaybackUrls(videoId);
    hlsUrl = fallback.hlsUrl;
    streamUrl = fallback.streamUrl;
  }

  const durationRaw = params.payload.Duration;
  const durationMs =
    typeof durationRaw === "number"
      ? Math.round(durationRaw * 1000)
      : typeof durationRaw === "string" && durationRaw
        ? Math.round(parseFloat(durationRaw) * 1000)
        : session.durationMs ?? undefined;

  await completeUploadSession({
    uploadId: session.id,
    cloudflareAssetId: videoId,
    durationMs,
  });

  await enqueueUploadProcessingJob({
    videoId: session.videoId,
    uploadId: session.id,
    cloudflareAssetId: videoId,
    hlsUrl: hlsUrl ?? streamUrl!,
    streamUrl: streamUrl ?? hlsUrl!,
    durationMs,
    thumbnailUrl: coverUrl ?? undefined,
  });

  return { duplicate: false, accepted: true };
}

/** @deprecated Use handleAlibabaVodWebhook */
export const handleCloudflareStreamWebhook = handleAlibabaVodWebhook;
