export type DirectUploadResult = {
  /** Alibaba VOD VideoId (column name legacy: cloudflareAssetId). */
  cloudflareAssetId: string;
  uploadUrl: string;
  expiresAt: Date;
  provider: "alibaba_vod" | "dev";
  /** Base64 UploadAuth from CreateUploadVideo (Alibaba). */
  uploadAuth: string | null;
  /** Base64 UploadAddress from CreateUploadVideo (Alibaba). */
  uploadAddress: string | null;
};

export type CreateUploadResult = {
  uploadId: string;
  videoId: string;
  uploadUrl: string;
  uploadToken: string;
  uploadUrlExpiresAt: string;
  status: string;
  provider: "alibaba_vod" | "dev";
  uploadAuth: string | null;
  uploadAddress: string | null;
};

export type PublishVideoResult = {
  videoId: string;
  status: string;
};

export type UploadProgressResult = {
  uploadId: string;
  videoId: string;
  status: string;
  bytesUploaded: string;
  fileSizeBytes: string;
};
