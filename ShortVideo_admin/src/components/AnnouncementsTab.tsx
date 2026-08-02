import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import {
  ApiError,
  createAnnouncement,
  deleteAnnouncement,
  fetchAnnouncements,
  updateAnnouncement,
} from "../api";
import type { AdminAnnouncement } from "../types";

export function AnnouncementsTab() {
  const [items, setItems] = useState<AdminAnnouncement[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [title, setTitle] = useState("");
  const [body, setBody] = useState("");
  const [isActive, setIsActive] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editTitle, setEditTitle] = useState("");
  const [editBody, setEditBody] = useState("");
  const [editIsActive, setEditIsActive] = useState(true);
  const [savingId, setSavingId] = useState<string | null>(null);
  const [deletingId, setDeletingId] = useState<string | null>(null);

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
      const announcement = await createAnnouncement({
        title: title.trim(),
        body: body.trim(),
        isActive,
      });
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

  function startEdit(announcement: AdminAnnouncement) {
    setEditingId(announcement.id);
    setEditTitle(announcement.title);
    setEditBody(announcement.body);
    setEditIsActive(announcement.isActive);
    setError(null);
  }

  function cancelEdit() {
    setEditingId(null);
    setEditTitle("");
    setEditBody("");
    setEditIsActive(true);
  }

  async function handleSaveEdit(id: string) {
    setSavingId(id);
    setError(null);
    try {
      const updated = await updateAnnouncement(id, {
        title: editTitle.trim(),
        body: editBody.trim(),
        isActive: editIsActive,
      });
      setItems((prev) => prev.map((item) => (item.id === id ? updated : item)));
      cancelEdit();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to update announcement.");
    } finally {
      setSavingId(null);
    }
  }

  async function handleDelete(announcement: AdminAnnouncement) {
    const confirmed = window.confirm(`Delete announcement "${announcement.title}"?`);
    if (!confirmed) return;

    setDeletingId(announcement.id);
    setError(null);
    try {
      await deleteAnnouncement(announcement.id);
      setItems((prev) => prev.filter((item) => item.id !== announcement.id));
      if (editingId === announcement.id) {
        cancelEdit();
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to delete announcement.");
    } finally {
      setDeletingId(null);
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
        {items.map((announcement) => {
          const isEditing = editingId === announcement.id;
          return (
            <div
              key={announcement.id}
              style={{ padding: "12px 0", borderBottom: "1px solid var(--border)" }}
            >
              {isEditing ? (
                <div>
                  <div className="field">
                    <label htmlFor={`edit-title-${announcement.id}`}>Title</label>
                    <input
                      id={`edit-title-${announcement.id}`}
                      required
                      value={editTitle}
                      onChange={(event) => setEditTitle(event.target.value)}
                    />
                  </div>
                  <div className="field">
                    <label htmlFor={`edit-body-${announcement.id}`}>Body</label>
                    <textarea
                      id={`edit-body-${announcement.id}`}
                      required
                      rows={4}
                      value={editBody}
                      onChange={(event) => setEditBody(event.target.value)}
                    />
                  </div>
                  <div
                    className="field"
                    style={{ flexDirection: "row", alignItems: "center", gap: 8 }}
                  >
                    <input
                      id={`edit-active-${announcement.id}`}
                      type="checkbox"
                      checked={editIsActive}
                      onChange={(event) => setEditIsActive(event.target.checked)}
                      style={{ width: "auto" }}
                    />
                    <label htmlFor={`edit-active-${announcement.id}`} style={{ margin: 0 }}>
                      Active
                    </label>
                  </div>
                  <div style={{ display: "flex", gap: 8 }}>
                    <button
                      className="btn btn-primary btn-sm"
                      type="button"
                      style={{ width: "auto" }}
                      disabled={savingId === announcement.id || !editTitle.trim() || !editBody.trim()}
                      onClick={() => void handleSaveEdit(announcement.id)}
                    >
                      {savingId === announcement.id ? "Saving…" : "Save"}
                    </button>
                    <button
                      className="btn btn-secondary btn-sm"
                      type="button"
                      disabled={savingId === announcement.id}
                      onClick={cancelEdit}
                    >
                      Cancel
                    </button>
                  </div>
                </div>
              ) : (
                <>
                  <div style={{ display: "flex", justifyContent: "space-between", gap: 12 }}>
                    <strong>{announcement.title}</strong>
                    <span
                      className={`badge ${announcement.isActive ? "badge-active" : "badge-suspended"}`}
                    >
                      {announcement.isActive ? "ACTIVE" : "INACTIVE"}
                    </span>
                  </div>
                  <p style={{ color: "var(--text-muted)", margin: "6px 0" }}>{announcement.body}</p>
                  <div
                    style={{
                      display: "flex",
                      justifyContent: "space-between",
                      alignItems: "center",
                      gap: 12,
                    }}
                  >
                    <small style={{ color: "var(--text-muted)" }}>
                      {new Date(announcement.createdAt).toLocaleString()}
                    </small>
                    <div style={{ display: "flex", gap: 8 }}>
                      <button
                        className="btn btn-secondary btn-sm"
                        type="button"
                        onClick={() => startEdit(announcement)}
                      >
                        Edit
                      </button>
                      <button
                        className="btn btn-danger btn-sm"
                        type="button"
                        disabled={deletingId === announcement.id}
                        onClick={() => void handleDelete(announcement)}
                      >
                        {deletingId === announcement.id ? "Deleting…" : "Delete"}
                      </button>
                    </div>
                  </div>
                </>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
