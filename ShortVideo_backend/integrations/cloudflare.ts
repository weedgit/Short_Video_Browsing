/**
 * @deprecated Cloudflare Stream is no longer used.
 * Re-exports Alibaba Cloud VOD + CDN integration for China deployments.
 */
export {
  isAlibabaVodConfigured,
  isCloudflareConfigured,
  createDirectUpload,
  fetchPlaybackUrls,
  buildAssetPlaybackUrls,
  buildAssetThumbnailUrl,
  resolveThumbnailUrl,
  resolveSignedPlaybackUrl,
} from "./alibaba-vod";
