import { completeUploadSession, recordWebhookEvent } from "../../db/repositories/upload.repository";
import { buildAssetPlaybackUrls } from "../../integrations/cloudflare";
import { enqueueUploadProcessingJob } from "../../jobs/queues";
import { hashPayload } from "../../utils/uploadToken";
import { getPrismaClient } from "../../db/client";

type CloudflareWebhookPayload = {
  uid?: string;
  status?: { state?: string };
  duration?: number;
  meta?: { uploadId?: string; videoId?: string };
};

export async function handleCloudflareStreamWebhook(params: {
  eventId: string;
  payload: CloudflareWebhookPayload;
  rawBody: string;
}): Promise<{ duplicate: boolean; accepted: boolean }> {
  const payloadHash = hashPayload(params.rawBody);
  const inserted = await recordWebhookEvent({
    provider: "cloudflare-stream",
    eventId: params.eventId,
    payloadHash,
  });

  if (!inserted) {
    return { duplicate: true, accepted: true };
  }

  const assetId = params.payload.uid;
  if (!assetId) {
    return { duplicate: false, accepted: false };
  }

  const prisma = getPrismaClient();
  const session = await prisma.uploadSession.findFirst({
    where: { cloudflareAssetId: assetId },
  });

  if (!session) {
    return { duplicate: false, accepted: false };
  }

  const state = params.payload.status?.state ?? "ready";
  if (state !== "ready") {
    return { duplicate: false, accepted: true };
  }

  const urls = buildAssetPlaybackUrls(assetId);
  const durationMs = params.payload.duration ? Math.round(params.payload.duration * 1000) : session.durationMs ?? undefined;

  await completeUploadSession({
    uploadId: session.id,
    cloudflareAssetId: assetId,
    durationMs,
  });

  await enqueueUploadProcessingJob({
    videoId: session.videoId,
    uploadId: session.id,
    cloudflareAssetId: assetId,
    hlsUrl: urls.hlsUrl,
    streamUrl: urls.streamUrl,
    durationMs,
  });

  return { duplicate: false, accepted: true };
}
