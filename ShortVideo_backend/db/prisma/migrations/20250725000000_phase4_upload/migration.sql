-- CreateEnum
CREATE TYPE "UploadStatus" AS ENUM ('DRAFT', 'UPLOADING', 'UPLOADED', 'PROCESSING', 'PUBLISHED', 'FAILED');

-- AlterTable
ALTER TABLE "videos" ALTER COLUMN "description" SET DEFAULT '';
ALTER TABLE "videos" ALTER COLUMN "duration_ms" SET DEFAULT 0;
ALTER TABLE "videos" ALTER COLUMN "status" SET DEFAULT 'PROCESSING';

-- CreateTable
CREATE TABLE "upload_sessions" (
    "id" TEXT NOT NULL,
    "user_id" TEXT NOT NULL,
    "video_id" TEXT NOT NULL,
    "status" "UploadStatus" NOT NULL DEFAULT 'DRAFT',
    "mime_type" TEXT NOT NULL,
    "file_size_bytes" BIGINT NOT NULL,
    "duration_ms" INTEGER,
    "upload_url" TEXT NOT NULL,
    "upload_token_hash" TEXT NOT NULL,
    "upload_url_expires_at" TIMESTAMP(3) NOT NULL,
    "upload_url_used_at" TIMESTAMP(3),
    "bytes_uploaded" BIGINT NOT NULL DEFAULT 0,
    "cloudflare_asset_id" TEXT,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "upload_sessions_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "webhook_events" (
    "id" TEXT NOT NULL,
    "provider" TEXT NOT NULL,
    "event_id" TEXT NOT NULL,
    "payload_hash" TEXT NOT NULL,
    "processed_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "webhook_events_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE UNIQUE INDEX "upload_sessions_video_id_key" ON "upload_sessions"("video_id");
CREATE UNIQUE INDEX "upload_sessions_upload_token_hash_key" ON "upload_sessions"("upload_token_hash");
CREATE INDEX "upload_sessions_user_id_status_idx" ON "upload_sessions"("user_id", "status");
CREATE UNIQUE INDEX "webhook_events_event_id_key" ON "webhook_events"("event_id");
CREATE INDEX "webhook_events_provider_processed_at_idx" ON "webhook_events"("provider", "processed_at");

-- AddForeignKey
ALTER TABLE "upload_sessions" ADD CONSTRAINT "upload_sessions_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "users"("id") ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE "upload_sessions" ADD CONSTRAINT "upload_sessions_video_id_fkey" FOREIGN KEY ("video_id") REFERENCES "videos"("id") ON DELETE CASCADE ON UPDATE CASCADE;
