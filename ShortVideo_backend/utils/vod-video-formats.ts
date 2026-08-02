/**
 * Alibaba ApsaraVideo VOD supported video containers.
 * @see https://www.alibabacloud.com/help/en/vod/product-overview/audio-and-video-upload
 *
 * Excludes M3U8 (playlist), GIF, and SWF — not suitable for single-file mobile upload.
 */
export const ALIBABA_VOD_VIDEO_EXTENSIONS = [
  "3gp",
  "asf",
  "avi",
  "dat",
  "dv",
  "f4v",
  "flv",
  "m2t",
  "m4v",
  "mj2",
  "mjpeg",
  "mkv",
  "mov",
  "mp4",
  "mpe",
  "mpeg",
  "mpg",
  "mts",
  "ogg",
  "ogv",
  "qt",
  "rm",
  "rmvb",
  "ts",
  "vob",
  "webm",
  "wmv",
] as const;

/** MIME types that map cleanly to Alibaba VOD video uploads. */
export const ALIBABA_VOD_VIDEO_MIME_TYPES = [
  "video/mp4",
  "video/quicktime",
  "video/webm",
  "video/x-msvideo",
  "video/avi",
  "video/msvideo",
  "video/x-matroska",
  "video/mpeg",
  "video/mpg",
  "video/3gpp",
  "video/3gpp2",
  "video/x-flv",
  "video/flv",
  "video/x-f4v",
  "video/x-ms-wmv",
  "video/x-ms-asf",
  "video/mp2t",
  "video/ogg",
  "video/dvd",
  "video/x-ms-vob",
  "video/x-dv",
  "video/dv",
  "video/x-pn-realvideo",
  "application/vnd.rn-realmedia",
  "application/vnd.rn-realmedia-vbr",
] as const;

const EXTENSION_SET = new Set<string>(
  ALIBABA_VOD_VIDEO_EXTENSIONS.map((ext) => ext.toLowerCase()),
);

const MIME_SET = new Set<string>(
  ALIBABA_VOD_VIDEO_MIME_TYPES.map((mime) => mime.toLowerCase()),
);

export const ALIBABA_VOD_VIDEO_FORMAT_HINT =
  "MP4, MOV, AVI, MKV, WEBM, FLV, 3GP, MPEG, TS, VOB, WMV";

export function extensionFromFileName(fileName: string | null | undefined): string | null {
  if (!fileName) return null;
  const base = fileName.split(/[\\/]/).pop() ?? fileName;
  const ext = base.includes(".") ? base.slice(base.lastIndexOf(".") + 1) : "";
  const normalized = ext.trim().toLowerCase();
  return normalized || null;
}

export function isAlibabaVodVideoExtension(extension: string | null | undefined): boolean {
  if (!extension) return false;
  return EXTENSION_SET.has(extension.trim().toLowerCase());
}

export function isAlibabaVodVideoMimeType(mimeType: string | null | undefined): boolean {
  if (!mimeType) return false;
  return MIME_SET.has(mimeType.trim().toLowerCase());
}

/** True when MIME and/or file extension is an Alibaba VOD video container. */
export function isAlibabaVodVideoType(params: {
  mimeType?: string | null;
  fileName?: string | null;
}): boolean {
  if (isAlibabaVodVideoMimeType(params.mimeType)) {
    return true;
  }
  return isAlibabaVodVideoExtension(extensionFromFileName(params.fileName));
}

export function vodExtensionFromMime(mimeType: string): string {
  const lower = mimeType.toLowerCase();
  if (lower.includes("quicktime") || lower.includes("mov")) return "mov";
  if (lower.includes("webm")) return "webm";
  if (lower.includes("avi") || lower.includes("msvideo")) return "avi";
  if (lower.includes("matroska") || lower.includes("mkv")) return "mkv";
  if (lower.includes("3gpp2")) return "3gp";
  if (lower.includes("3gpp")) return "3gp";
  if (lower.includes("flv") || lower.includes("f4v")) return "flv";
  if (lower.includes("wmv")) return "wmv";
  if (lower.includes("asf")) return "asf";
  if (lower.includes("mp2t") || lower === "video/ts") return "ts";
  if (lower.includes("vob") || lower.includes("dvd")) return "vob";
  if (lower.includes("ogg")) return "ogv";
  if (lower.includes("real") || lower.includes("rn-real")) return "rm";
  if (lower.includes("dv")) return "dv";
  if (lower.includes("mpeg") || lower.includes("mpg")) return "mpg";
  return "mp4";
}
