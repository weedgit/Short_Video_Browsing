import { Queue, Worker } from "bullmq";
import { config } from "../config";
import { finalizeProcessedVideo } from "../db/repositories/upload.repository";
import { logger } from "../utils/logger";

export type UploadProcessingJob = {
  videoId: string;
  uploadId: string;
  cloudflareAssetId: string;
  hlsUrl?: string;
  streamUrl?: string;
  durationMs?: number;
  thumbnailUrl?: string;
};

const queueName = "upload-processing";

let uploadQueue: Queue<UploadProcessingJob> | null = null;
let uploadWorker: Worker<UploadProcessingJob> | null = null;

function getConnection() {
  if (!config.redisUrl) {
    return undefined;
  }

  return { url: config.redisUrl };
}

export function getUploadProcessingQueue(): Queue<UploadProcessingJob> | null {
  const connection = getConnection();
  if (!connection) {
    return null;
  }

  if (!uploadQueue) {
    uploadQueue = new Queue<UploadProcessingJob>(queueName, { connection });
  }

  return uploadQueue;
}

export async function enqueueUploadProcessingJob(job: UploadProcessingJob): Promise<void> {
  const queue = getUploadProcessingQueue();
  if (!queue) {
    await processUploadJob(job);
    return;
  }

  await queue.add("process-upload", job, {
    attempts: 3,
    removeOnComplete: 100,
    removeOnFail: 100,
  });
}

export async function processUploadJob(job: UploadProcessingJob): Promise<void> {
  await finalizeProcessedVideo({
    videoId: job.videoId,
    cloudflareAssetId: job.cloudflareAssetId,
    hlsUrl: job.hlsUrl,
    streamUrl: job.streamUrl,
    durationMs: job.durationMs,
    thumbnailUrl: job.thumbnailUrl,
  });
}

export function startUploadWorkers(): void {
  const connection = getConnection();
  if (!connection || uploadWorker) {
    return;
  }

  uploadWorker = new Worker<UploadProcessingJob>(
    queueName,
    async (job) => {
      await processUploadJob(job.data);
    },
    { connection },
  );

  uploadWorker.on("failed", (job, error) => {
    logger.error({ jobId: job?.id, error }, "Upload processing job failed");
  });
}

export async function stopUploadWorkers(): Promise<void> {
  await uploadWorker?.close();
  await uploadQueue?.close();
  uploadWorker = null;
  uploadQueue = null;
}
