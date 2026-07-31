import { useEffect, useState } from "react";
import { ApiError, fetchVideos, updateVideoStatus } from "../api";
import type { AdminVideo } from "../types";
import { StatusBadge } from "./StatusBadge";

const STATUS_OPTIONS: Array<AdminVideo["status"] | ""> = ["", "PROCESSING", "READY", "FAILED", "DELETED"];

export function VideosTab() {
  const [items, setItems] = useState<AdminVideo[]>([]);
  const [cursor, setCursor] = useState<string | null>(null);
  const [hasMore, setHasMore] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<AdminVideo["status"] | "">("");

  async function load(nextCursor?: string, filter = statusFilter) {
    setLoading(true);
    setError(null);
    try {
      const page = await fetchVideos(nextCursor, filter || undefined);
      setItems((prev) => (nextCursor ? [...prev, ...page.items] : page.items));
      setCursor(page.nextCursor);
      setHasMore(page.hasMore);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load videos.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load(undefined, statusFilter);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [statusFilter]);

  async function handleStatusChange(video: AdminVideo, status: AdminVideo["status"]) {
    try {
      const updated = await updateVideoStatus(video.id, status);
      setItems((prev) =>
        prev.map((item) => (item.id === video.id ? { ...item, status: updated.status } : item)),
      );
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to update video.");
    }
  }

  return (
    <div>
      <div className="page-header">
        <h1>Videos</h1>
      </div>

      {error && <div className="error-banner">{error}</div>}

      <div className="toolbar">
        <label htmlFor="status-filter" style={{ fontSize: 13, color: "var(--text-muted)" }}>
          Filter by status
        </label>
        <select
          id="status-filter"
          className="select-inline"
          value={statusFilter}
          onChange={(event) => setStatusFilter(event.target.value as AdminVideo["status"] | "")}
        >
          {STATUS_OPTIONS.map((option) => (
            <option key={option || "all"} value={option}>
              {option || "All"}
            </option>
          ))}
        </select>
      </div>

      <div className="card">
        <table>
          <thead>
            <tr>
              <th>Thumbnail</th>
              <th>Description</th>
              <th>Author</th>
              <th>Likes</th>
              <th>Status</th>
              <th>Created</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            {items.map((video) => (
              <tr key={video.id}>
                <td>
                  {video.thumbnailUrl ? (
                    <img className="thumb" src={video.thumbnailUrl} alt="" />
                  ) : (
                    <div className="thumb" />
                  )}
                </td>
                <td style={{ maxWidth: 320 }}>{video.description || <em>(no description)</em>}</td>
                <td>{video.authorName}</td>
                <td>{video.likeCount}</td>
                <td>
                  <StatusBadge status={video.status} />
                </td>
                <td>{new Date(video.createdAt).toLocaleDateString()}</td>
                <td>
                  <select
                    className="select-inline"
                    value={video.status}
                    onChange={(event) =>
                      handleStatusChange(video, event.target.value as AdminVideo["status"])
                    }
                  >
                    <option value="PROCESSING">PROCESSING</option>
                    <option value="READY">READY</option>
                    <option value="FAILED">FAILED</option>
                    <option value="DELETED">DELETED</option>
                  </select>
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        {loading && <p className="loading-state">Loading…</p>}
        {!loading && items.length === 0 && <p className="empty-state">No videos found.</p>}
      </div>

      {hasMore && cursor && (
        <div className="pagination">
          <button className="btn btn-secondary" onClick={() => load(cursor)} disabled={loading}>
            Load more
          </button>
        </div>
      )}
    </div>
  );
}
