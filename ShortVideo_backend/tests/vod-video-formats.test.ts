import { describe, expect, it } from "vitest";
import {
  isAlibabaVodVideoExtension,
  isAlibabaVodVideoMimeType,
  isAlibabaVodVideoType,
  vodExtensionFromMime,
} from "../utils/vod-video-formats";

describe("Alibaba VOD video formats", () => {
  it("accepts main MPEG extensions including MP4 and VOB", () => {
    expect(isAlibabaVodVideoExtension("mp4")).toBe(true);
    expect(isAlibabaVodVideoExtension("VOB")).toBe(true);
    expect(isAlibabaVodVideoExtension("mov")).toBe(true);
    expect(isAlibabaVodVideoExtension("xyz")).toBe(false);
  });

  it("accepts mapped MIME types and rejects unknown video MIME", () => {
    expect(isAlibabaVodVideoMimeType("video/mp4")).toBe(true);
    expect(isAlibabaVodVideoMimeType("video/dvd")).toBe(true);
    expect(isAlibabaVodVideoMimeType("video/x-unknown")).toBe(false);
    expect(isAlibabaVodVideoMimeType("image/png")).toBe(false);
  });

  it("resolves CreateUploadVideo filename extensions from MIME", () => {
    expect(vodExtensionFromMime("video/mp4")).toBe("mp4");
    expect(vodExtensionFromMime("video/dvd")).toBe("vob");
    expect(vodExtensionFromMime("video/quicktime")).toBe("mov");
    expect(vodExtensionFromMime("video/mp2t")).toBe("ts");
  });

  it("allows type by extension when MIME is generic", () => {
    expect(
      isAlibabaVodVideoType({ mimeType: "application/octet-stream", fileName: "clip.vob" }),
    ).toBe(true);
  });
});
