-- AlterTable
ALTER TABLE "video_comments" ADD COLUMN "parent_id" TEXT;

-- CreateIndex
CREATE INDEX "video_comments_video_id_parent_id_created_at_idx" ON "video_comments"("video_id", "parent_id", "created_at");

-- CreateIndex
CREATE INDEX "video_comments_parent_id_idx" ON "video_comments"("parent_id");

-- AddForeignKey
ALTER TABLE "video_comments" ADD CONSTRAINT "video_comments_parent_id_fkey" FOREIGN KEY ("parent_id") REFERENCES "video_comments"("id") ON DELETE CASCADE ON UPDATE CASCADE;
