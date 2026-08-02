import { FormEvent, useEffect, useRef, useState } from "react";
import { ApiError, fetchUserProfile, fetchVideo, fetchVideos, updateVideoStatus } from "../api";
import type { AdminUserProfile, AdminVideo } from "../types";
import { PaginationBar } from "./PaginationBar";
import { StatusBadge } from "./StatusBadge";

const STATUS_OPTIONS: Array<AdminVideo["status"] | ""> = ["", "PROCESSING", "READY", "FAILED", "DELETED"];

/** Fill complete grid rows so trailing empty cells don't appear while more pages exist. */
const GRID_ROWS_PER_PAGE = 3;
const GRID_MIN_COL_PX = 180;
const GRID_GAP_PX = 16;

const VIDEO_CATEGORIES = [
  "Comedy",
  "Dance",
  "Beauty & Style",
  "Food",
  "Sports",
  "Animals",
  "Gaming",
  "Music",
  "Fashion",
  "Education",
  "Travel",
  "DIY & Life Hacks",
  "Autos",
  "Science & Tech",
  "Entertainment",
  "Family",
  "Art",
  "Fitness & Health",
  "ASMR",
  "News & Politics",
] as const;

function formatDuration(ms: number): string {
  if (!ms || ms < 0) return "—";
  const totalSec = Math.round(ms / 1000);
  const m = Math.floor(totalSec / 60);
  const s = totalSec % 60;
  return `${m}:${s.toString().padStart(2, "0")}`;
}

function formatHashtag(tag: string): string {
  const bare = tag.replace(/^#/, "");
  return `#${bare}`;
}

type VideoTileProps = {
  video: AdminVideo;
  onOpen: (video: AdminVideo) => void;
  onAuthor: (userId: string) => void;
  onHashtag: (tag: string) => void;
  onCategory: (category: string) => void;
  onStatusChange: (video: AdminVideo, status: AdminVideo["status"]) => void;
};

function VideoTile({
  video,
  onOpen,
  onAuthor,
  onHashtag,
  onCategory,
  onStatusChange,
}: VideoTileProps) {
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const [hovering, setHovering] = useState(false);

  useEffect(() => {
    const el = videoRef.current;
    if (!el) return;
    if (hovering && video.streamUrl) {
      el.currentTime = 0;
      void el.play().catch(() => {
        // Autoplay may be blocked; keep poster/thumbnail visible.
      });
    } else {
      el.pause();
      el.currentTime = 0;
    }
  }, [hovering, video.streamUrl]);

  return (
    <article
      className={`video-tile ${hovering ? "is-hovering" : ""}`}
      onMouseEnter={() => setHovering(true)}
      onMouseLeave={() => setHovering(false)}
    >
      <div className="video-tile-media">
        {video.thumbnailUrl ? (
          <img className="video-tile-poster" src={video.thumbnailUrl} alt="" />
        ) : (
          <div className="video-tile-poster video-tile-poster-empty" />
        )}
        {video.streamUrl ? (
          <video
            ref={videoRef}
            className="video-tile-preview"
            src={video.streamUrl}
            muted
            loop
            playsInline
            preload="metadata"
            poster={video.thumbnailUrl ?? undefined}
          />
        ) : null}

        <button
          type="button"
          className="video-tile-hit"
          onClick={() => onOpen(video)}
          title="View video"
          aria-label="Open video detail"
        />

        <span className="video-tile-status">
          <StatusBadge status={video.status} />
        </span>
        <span className="video-tile-duration">{formatDuration(video.durationMs)}</span>

        <div className="video-tile-body">
          <button type="button" className="video-tile-title" onClick={() => onOpen(video)}>
            {video.description || <em>(no description)</em>}
          </button>

          <div className="video-tile-meta">
            <button type="button" className="video-tile-author" onClick={() => onAuthor(video.userId)}>
              {video.authorName}
            </button>
            <span>· {video.likeCount} likes</span>
          </div>

          <div className="video-tile-tags">
            {video.category ? (
              <button type="button" className="tag-chip" onClick={() => onCategory(video.category!)}>
                {video.category}
              </button>
            ) : null}
            {video.hashtags?.slice(0, 2).map((tag) => (
              <button key={tag} type="button" className="tag-chip" onClick={() => onHashtag(tag)}>
                {formatHashtag(tag)}
              </button>
            ))}
          </div>

          <select
            className="select-inline video-tile-action"
            value={video.status}
            onChange={(event) => onStatusChange(video, event.target.value as AdminVideo["status"])}
            aria-label={`Status for ${video.description || video.id}`}
          >
            <option value="PROCESSING">PROCESSING</option>
            <option value="READY">READY</option>
            <option value="FAILED">FAILED</option>
            <option value="DELETED">DELETED</option>
          </select>
        </div>
      </div>
    </article>
  );
}

export function VideosTab() {
  const gridRef = useRef<HTMLDivElement | null>(null);
  const [items, setItems] = useState<AdminVideo[]>([]);
  const [page, setPage] = useState(1);
  const [total, setTotal] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [limit, setLimit] = useState(0);
  const [pageSize, setPageSize] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [statusFilter, setStatusFilter] = useState<AdminVideo["status"] | "">("");
  const [categoryFilter, setCategoryFilter] = useState("");
  const [hashtagInput, setHashtagInput] = useState("");
  const [searchInput, setSearchInput] = useState("");
  const [appliedQ, setAppliedQ] = useState("");
  const [appliedHashtag, setAppliedHashtag] = useState("");

  const [selectedVideo, setSelectedVideo] = useState<AdminVideo | null>(null);
  const [videoDetailLoading, setVideoDetailLoading] = useState(false);
  const [selectedProfile, setSelectedProfile] = useState<AdminUserProfile | null>(null);
  const [profileLoading, setProfileLoading] = useState(false);

  useEffect(() => {
    const el = gridRef.current;
    if (!el) return;

    const measure = () => {
      const raw = getComputedStyle(el).gridTemplateColumns.trim();
      const tracks =
        !raw || raw === "none" ? 0 : raw.split(/\s+/).filter(Boolean).length;
      const width = el.clientWidth;
      const inferred = Math.max(
        1,
        Math.floor((width + GRID_GAP_PX) / (GRID_MIN_COL_PX + GRID_GAP_PX)),
      );
      const columns = Math.max(1, tracks || inferred);
      const next = Math.min(100, columns * GRID_ROWS_PER_PAGE);
      setPageSize((prev) => (prev === next ? prev : next));
    };

    if (typeof ResizeObserver === "undefined") {
      measure();
      return;
    }

    const observer = new ResizeObserver(measure);
    observer.observe(el);
    measure();
    return () => observer.disconnect();
  }, []);

  async function load(
    pageNumber = page,
    filters = {
      status: statusFilter,
      q: appliedQ,
      hashtag: appliedHashtag,
      category: categoryFilter,
    },
    limitOverride = pageSize,
  ) {
    if (limitOverride < 1) return;
    setLoading(true);
    setError(null);
    try {
      const result = await fetchVideos(
        pageNumber,
        {
          status: filters.status || undefined,
          q: filters.q || undefined,
          hashtag: filters.hashtag || undefined,
          category: filters.category || undefined,
        },
        limitOverride,
      );
      setItems(result.items);
      setPage(result.page);
      setTotal(result.total);
      setTotalPages(result.totalPages);
      setLimit(result.limit);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load videos.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (pageSize < 1) return;
    void load(page, {
      status: statusFilter,
      q: appliedQ,
      hashtag: appliedHashtag,
      category: categoryFilter,
    }, pageSize);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, pageSize, statusFilter, categoryFilter, appliedQ, appliedHashtag]);

  function handleSearch(event: FormEvent) {
    event.preventDefault();
    setPage(1);
    setAppliedQ(searchInput.trim());
    setAppliedHashtag(hashtagInput.trim().replace(/^#/, ""));
  }

  function clearFilters() {
    setSearchInput("");
    setHashtagInput("");
    setAppliedQ("");
    setAppliedHashtag("");
    setStatusFilter("");
    setCategoryFilter("");
    setPage(1);
  }

  async function handleStatusChange(video: AdminVideo, status: AdminVideo["status"]) {
    try {
      const updated = await updateVideoStatus(video.id, status);
      setItems((prev) =>
        prev.map((item) => (item.id === video.id ? { ...item, status: updated.status } : item)),
      );
      if (selectedVideo?.id === video.id) {
        setSelectedVideo((prev) => (prev ? { ...prev, status: updated.status } : prev));
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to update video.");
    }
  }

  async function openVideoDetail(video: AdminVideo) {
    setSelectedProfile(null);
    setSelectedVideo(video);
    setVideoDetailLoading(true);
    try {
      const detail = await fetchVideo(video.id);
      setSelectedVideo(detail);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load video detail.");
    } finally {
      setVideoDetailLoading(false);
    }
  }

  async function openUserProfile(userId: string) {
    setSelectedVideo(null);
    setProfileLoading(true);
    setSelectedProfile(null);
    try {
      const profile = await fetchUserProfile(userId);
      setSelectedProfile(profile);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load user profile.");
    } finally {
      setProfileLoading(false);
    }
  }

  function filterByHashtag(tag: string) {
    const bare = tag.replace(/^#/, "");
    setHashtagInput(bare);
    setAppliedHashtag(bare);
    setPage(1);
    setSelectedVideo(null);
  }

  function filterByCategory(category: string) {
    setCategoryFilter(category);
    setPage(1);
    setSelectedVideo(null);
  }

  return (
    <div>
      <div className="page-header">
        <h1>Videos</h1>
      </div>

      {error && <div className="error-banner">{error}</div>}

      <form className="toolbar toolbar-wrap" onSubmit={handleSearch}>
        <input
          className="input-inline"
          type="search"
          placeholder="Search description, author, #tag…"
          value={searchInput}
          onChange={(event) => setSearchInput(event.target.value)}
          aria-label="Search videos"
        />
        <input
          className="input-inline input-narrow"
          type="search"
          placeholder="Hashtag"
          value={hashtagInput}
          onChange={(event) => setHashtagInput(event.target.value)}
          aria-label="Filter by hashtag"
        />
        <select
          className="select-inline"
          value={categoryFilter}
          onChange={(event) => {
            setPage(1);
            setCategoryFilter(event.target.value);
          }}
          aria-label="Filter by category"
        >
          <option value="">All categories</option>
          {VIDEO_CATEGORIES.map((category) => (
            <option key={category} value={category}>
              {category}
            </option>
          ))}
        </select>
        <select
          className="select-inline"
          value={statusFilter}
          onChange={(event) => {
            setPage(1);
            setStatusFilter(event.target.value as AdminVideo["status"] | "");
          }}
          aria-label="Filter by status"
        >
          {STATUS_OPTIONS.map((option) => (
            <option key={option || "all"} value={option}>
              {option || "All statuses"}
            </option>
          ))}
        </select>
        <button type="submit" className="btn btn-primary">
          Search
        </button>
        <button type="button" className="btn btn-secondary" onClick={clearFilters}>
          Clear
        </button>
      </form>

      <div className="video-grid" ref={gridRef}>
        {items.map((video) => (
          <VideoTile
            key={video.id}
            video={video}
            onOpen={(item) => void openVideoDetail(item)}
            onAuthor={(userId) => void openUserProfile(userId)}
            onHashtag={filterByHashtag}
            onCategory={filterByCategory}
            onStatusChange={(item, status) => void handleStatusChange(item, status)}
          />
        ))}
      </div>

      {loading && <p className="loading-state">Loading…</p>}
      {!loading && pageSize > 0 && items.length === 0 && <p className="empty-state">No videos found.</p>}

      <PaginationBar
        page={page}
        totalPages={totalPages}
        total={total}
        limit={limit || pageSize}
        itemCount={items.length}
        loading={loading}
        onPageChange={setPage}
        noun="videos"
      />

      {(selectedVideo || videoDetailLoading) && (
        <div className="modal-backdrop" onClick={() => setSelectedVideo(null)} role="presentation">
          <div
            className="modal-panel modal-video"
            onClick={(event) => event.stopPropagation()}
            role="dialog"
            aria-modal="true"
            aria-label="Video detail"
          >
            <div className="modal-header">
              <h2>Video detail</h2>
              <button type="button" className="modal-close" onClick={() => setSelectedVideo(null)}>
                ×
              </button>
            </div>
            {videoDetailLoading && !selectedVideo?.streamUrl ? (
              <p className="loading-state">Loading…</p>
            ) : selectedVideo ? (
              <div className="video-detail-grid">
                <div className="video-player-wrap">
                  {selectedVideo.streamUrl ? (
                    <video
                      key={selectedVideo.streamUrl}
                      className="video-player"
                      src={selectedVideo.streamUrl}
                      controls
                      autoPlay
                      playsInline
                      poster={selectedVideo.thumbnailUrl ?? undefined}
                    />
                  ) : (
                    <div className="video-player-empty">
                      {selectedVideo.thumbnailUrl ? (
                        <img src={selectedVideo.thumbnailUrl} alt="" />
                      ) : null}
                      <p>Playback unavailable</p>
                    </div>
                  )}
                </div>
                <div className="video-detail-info">
                  <dl className="detail-list detail-list-separated">
                    <div>
                      <dt>Author</dt>
                      <dd>
                        <button
                          type="button"
                          className="link-btn"
                          onClick={() => void openUserProfile(selectedVideo.userId)}
                        >
                          {selectedVideo.authorName}
                        </button>
                        <span className="muted"> @{selectedVideo.authorUsername}</span>
                      </dd>
                    </div>
                    <div>
                      <dt>Description</dt>
                      <dd>{selectedVideo.description || "—"}</dd>
                    </div>
                    <div>
                      <dt>Category</dt>
                      <dd>
                        {selectedVideo.category ? (
                          <button
                            type="button"
                            className="link-btn"
                            onClick={() => filterByCategory(selectedVideo.category!)}
                          >
                            {selectedVideo.category}
                          </button>
                        ) : (
                          "—"
                        )}
                      </dd>
                    </div>
                    <div>
                      <dt>Hashtags</dt>
                      <dd>
                        {selectedVideo.hashtags?.length ? (
                          <div className="tag-row">
                            {selectedVideo.hashtags.map((tag) => (
                              <button
                                key={tag}
                                type="button"
                                className="tag-chip"
                                onClick={() => filterByHashtag(tag)}
                              >
                                {formatHashtag(tag)}
                              </button>
                            ))}
                          </div>
                        ) : (
                          "—"
                        )}
                      </dd>
                    </div>
                    <div>
                      <dt>Music</dt>
                      <dd>{selectedVideo.musicLabel || "—"}</dd>
                    </div>
                    <div>
                      <dt>Likes</dt>
                      <dd>{selectedVideo.likeCount.toLocaleString()}</dd>
                    </div>
                    <div>
                      <dt>Comments</dt>
                      <dd>{selectedVideo.commentCount.toLocaleString()}</dd>
                    </div>
                    <div>
                      <dt>Shares</dt>
                      <dd>{selectedVideo.shareCount.toLocaleString()}</dd>
                    </div>
                    <div>
                      <dt>Duration</dt>
                      <dd>{formatDuration(selectedVideo.durationMs)}</dd>
                    </div>
                    <div>
                      <dt>Status</dt>
                      <dd>
                        <StatusBadge status={selectedVideo.status} />
                      </dd>
                    </div>
                    <div>
                      <dt>Created</dt>
                      <dd>{new Date(selectedVideo.createdAt).toLocaleString()}</dd>
                    </div>
                  </dl>
                </div>
              </div>
            ) : null}
          </div>
        </div>
      )}

      {(selectedProfile || profileLoading) && (
        <div className="modal-backdrop" onClick={() => setSelectedProfile(null)} role="presentation">
          <div
            className="modal-panel modal-profile"
            onClick={(event) => event.stopPropagation()}
            role="dialog"
            aria-modal="true"
            aria-label="User profile"
          >
            <div className="modal-header">
              <h2>User profile</h2>
              <button type="button" className="modal-close" onClick={() => setSelectedProfile(null)}>
                ×
              </button>
            </div>
            {profileLoading && !selectedProfile ? (
              <p className="loading-state">Loading…</p>
            ) : selectedProfile ? (
              <div className="profile-detail">
                <div className="profile-hero">
                  {selectedProfile.avatarUrl ? (
                    <img className="profile-avatar" src={selectedProfile.avatarUrl} alt="" />
                  ) : (
                    <div className="profile-avatar profile-avatar-fallback">
                      {selectedProfile.displayName.slice(0, 1).toUpperCase()}
                    </div>
                  )}
                  <div>
                    <h3>{selectedProfile.displayName}</h3>
                    <p className="muted">@{selectedProfile.username}</p>
                  </div>
                </div>
                {selectedProfile.bio && <p className="profile-bio">{selectedProfile.bio}</p>}
                <div className="profile-stats">
                  <div>
                    <strong>{selectedProfile.videoCount}</strong>
                    <span>Videos</span>
                  </div>
                  <div>
                    <strong>{selectedProfile.followerCount}</strong>
                    <span>Followers</span>
                  </div>
                  <div>
                    <strong>{selectedProfile.followingCount}</strong>
                    <span>Following</span>
                  </div>
                  <div>
                    <strong>{selectedProfile.likeCount}</strong>
                    <span>Likes</span>
                  </div>
                </div>
                <dl className="detail-list">
                  <div>
                    <dt>Email</dt>
                    <dd>{selectedProfile.email}</dd>
                  </div>
                  <div>
                    <dt>Role</dt>
                    <dd>{selectedProfile.role}</dd>
                  </div>
                  <div>
                    <dt>Status</dt>
                    <dd>
                      <StatusBadge status={selectedProfile.status} />
                    </dd>
                  </div>
                  <div>
                    <dt>Joined</dt>
                    <dd>{new Date(selectedProfile.createdAt).toLocaleString()}</dd>
                  </div>
                </dl>
              </div>
            ) : null}
          </div>
        </div>
      )}
    </div>
  );
}
