-- CreateEnum
CREATE TYPE "VideoStatus" AS ENUM ('PROCESSING', 'READY', 'FAILED', 'DELETED');

-- CreateTable
CREATE TABLE "videos" (
    "id" TEXT NOT NULL,
    "user_id" TEXT NOT NULL,
    "description" TEXT NOT NULL,
    "duration_ms" INTEGER NOT NULL,
    "status" "VideoStatus" NOT NULL DEFAULT 'READY',
    "cloudflare_asset_id" TEXT,
    "stream_url" TEXT,
    "hls_url" TEXT,
    "category" TEXT,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP(3) NOT NULL,
    "deleted_at" TIMESTAMP(3),

    CONSTRAINT "videos_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "video_hashtags" (
    "id" TEXT NOT NULL,
    "video_id" TEXT NOT NULL,
    "tag" TEXT NOT NULL,

    CONSTRAINT "video_hashtags_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "feed_impressions" (
    "id" TEXT NOT NULL,
    "video_id" TEXT NOT NULL,
    "user_id" TEXT,
    "device_id" TEXT,
    "impressed_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "feed_impressions_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "playback_events" (
    "id" TEXT NOT NULL,
    "video_id" TEXT NOT NULL,
    "user_id" TEXT,
    "device_id" TEXT,
    "event_type" TEXT NOT NULL,
    "position_ms" INTEGER NOT NULL DEFAULT 0,
    "occurred_at" TIMESTAMP(3) NOT NULL,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "playback_events_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE INDEX "videos_status_created_at_id_idx" ON "videos"("status", "created_at", "id");

-- CreateIndex
CREATE INDEX "videos_user_id_idx" ON "videos"("user_id");

-- CreateIndex
CREATE UNIQUE INDEX "video_hashtags_video_id_tag_key" ON "video_hashtags"("video_id", "tag");

-- CreateIndex
CREATE INDEX "video_hashtags_tag_idx" ON "video_hashtags"("tag");

-- CreateIndex
CREATE INDEX "feed_impressions_user_id_impressed_at_idx" ON "feed_impressions"("user_id", "impressed_at");

-- CreateIndex
CREATE INDEX "feed_impressions_device_id_impressed_at_idx" ON "feed_impressions"("device_id", "impressed_at");

-- CreateIndex
CREATE INDEX "feed_impressions_video_id_idx" ON "feed_impressions"("video_id");

-- CreateIndex
CREATE INDEX "playback_events_video_id_occurred_at_idx" ON "playback_events"("video_id", "occurred_at");

-- CreateIndex
CREATE INDEX "playback_events_user_id_occurred_at_idx" ON "playback_events"("user_id", "occurred_at");

-- CreateIndex
CREATE INDEX "playback_events_device_id_occurred_at_idx" ON "playback_events"("device_id", "occurred_at");

-- AddForeignKey
ALTER TABLE "videos" ADD CONSTRAINT "videos_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "users"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "video_hashtags" ADD CONSTRAINT "video_hashtags_video_id_fkey" FOREIGN KEY ("video_id") REFERENCES "videos"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "feed_impressions" ADD CONSTRAINT "feed_impressions_video_id_fkey" FOREIGN KEY ("video_id") REFERENCES "videos"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "feed_impressions" ADD CONSTRAINT "feed_impressions_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "users"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "playback_events" ADD CONSTRAINT "playback_events_video_id_fkey" FOREIGN KEY ("video_id") REFERENCES "videos"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "playback_events" ADD CONSTRAINT "playback_events_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "users"("id") ON DELETE CASCADE ON UPDATE CASCADE;
