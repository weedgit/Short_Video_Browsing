export type DirectUploadResult = {
  uploadUrl: string;
  cloudflareAssetId: string;
  expiresAt: Date;
};

export type CreateUploadResult = {
  uploadId: string;
  videoId: string;
  uploadUrl: string;
  uploadToken: string;
  uploadUrlExpiresAt: string;
  status: string;
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
