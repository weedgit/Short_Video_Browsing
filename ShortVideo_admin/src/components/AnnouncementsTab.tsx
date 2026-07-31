import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { ApiError, createAnnouncement, fetchAnnouncements } from "../api";
import type { AdminAnnouncement } from "../types";

export function AnnouncementsTab() {
  const [items, setItems] = useState<AdminAnnouncement[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [title, setTitle] = useState("");
  const [body, setBody] = useState("");
  const [isActive, setIsActive] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  async function load() {
    setLoading(true);
    setError(null);
    try {
      const page = await fetchAnnouncements();
      setItems(page.items);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load announcements.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load();
  }, []);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      const announcement = await createAnnouncement({ title: title.trim(), body: body.trim(), isActive });
      setItems((prev) => [announcement, ...prev]);
      setTitle("");
      setBody("");
      setIsActive(true);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to publish announcement.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div>
      <div className="page-header">
        <h1>Announcements</h1>
      </div>

      {error && <div className="error-banner">{error}</div>}

      <div className="card">
        <h3 style={{ marginTop: 0 }}>New announcement</h3>
        <form onSubmit={handleSubmit}>
          <div className="field">
            <label htmlFor="announcement-title">Title</label>
            <input
              id="announcement-title"
              required
              value={title}
              onChange={(event) => setTitle(event.target.value)}
            />
          </div>
          <div className="field">
            <label htmlFor="announcement-body">Body</label>
            <textarea
              id="announcement-body"
              required
              rows={4}
              value={body}
              onChange={(event) => setBody(event.target.value)}
            />
          </div>
          <div className="field" style={{ flexDirection: "row", alignItems: "center", gap: 8 }}>
            <input
              id="announcement-active"
              type="checkbox"
              checked={isActive}
              onChange={(event) => setIsActive(event.target.checked)}
              style={{ width: "auto" }}
            />
            <label htmlFor="announcement-active" style={{ margin: 0 }}>
              Publish immediately (active)
            </label>
          </div>
          <button className="btn btn-primary" type="submit" disabled={submitting} style={{ width: "auto" }}>
            {submitting ? "Publishing…" : "Publish announcement"}
          </button>
        </form>
      </div>

      <div className="card">
        <h3 style={{ marginTop: 0 }}>Published announcements</h3>
        {loading && <p className="loading-state">Loading…</p>}
        {!loading && items.length === 0 && <p className="empty-state">No announcements yet.</p>}
        {items.map((announcement) => (
          <div key={announcement.id} style={{ padding: "12px 0", borderBottom: "1px solid var(--border)" }}>
            <div style={{ display: "flex", justifyContent: "space-between" }}>
              <strong>{announcement.title}</strong>
              <span className={`badge ${announcement.isActive ? "badge-active" : "badge-suspended"}`}>
                {announcement.isActive ? "ACTIVE" : "INACTIVE"}
              </span>
            </div>
            <p style={{ color: "var(--text-muted)", margin: "6px 0" }}>{announcement.body}</p>
            <small style={{ color: "var(--text-muted)" }}>
              {new Date(announcement.createdAt).toLocaleString()}
            </small>
          </div>
        ))}
      </div>
    </div>
  );
}
