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
import { PaginationBar } from "./PaginationBar";

export function AnnouncementsTab() {
  const [items, setItems] = useState<AdminAnnouncement[]>([]);
  const [page, setPage] = useState(1);
  const [total, setTotal] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [limit, setLimit] = useState(20);
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

  const [searchInput, setSearchInput] = useState("");
  const [appliedQ, setAppliedQ] = useState("");
  const [activeFilter, setActiveFilter] = useState<"" | "true" | "false">("");
  const [showComposer, setShowComposer] = useState(false);

  async function load(
    pageNumber = page,
    filters = {
      q: appliedQ,
      active: activeFilter,
    },
  ) {
    setLoading(true);
    setError(null);
    try {
      const result = await fetchAnnouncements(pageNumber, {
        q: filters.q || undefined,
        active: filters.active || undefined,
      });
      setItems(result.items);
      setPage(result.page);
      setTotal(result.total);
      setTotalPages(result.totalPages);
      setLimit(result.limit);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load announcements.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load(page, {
      q: appliedQ,
      active: activeFilter,
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, appliedQ, activeFilter]);

  function handleSearch(event: FormEvent) {
    event.preventDefault();
    setPage(1);
    setAppliedQ(searchInput.trim());
  }

  function clearFilters() {
    setSearchInput("");
    setAppliedQ("");
    setActiveFilter("");
    setPage(1);
  }

  function openComposer() {
    setShowComposer(true);
    setError(null);
  }

  function closeComposer() {
    setShowComposer(false);
    setTitle("");
    setBody("");
    setIsActive(true);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      await createAnnouncement({
        title: title.trim(),
        body: body.trim(),
        isActive,
      });
      closeComposer();
      setPage(1);
      await load(1);
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
      if (editingId === announcement.id) {
        cancelEdit();
      }
      await load(page);
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
        <div className="header-actions">
          <button type="button" className="btn btn-primary" onClick={openComposer} style={{ width: "auto" }}>
            New announcement
          </button>
        </div>
      </div>

      {error && <div className="error-banner">{error}</div>}

      <form className="toolbar toolbar-wrap" onSubmit={handleSearch}>
        <input
          className="input-inline"
          type="search"
          placeholder="Search title or body…"
          value={searchInput}
          onChange={(event) => setSearchInput(event.target.value)}
          aria-label="Search announcements"
        />
        <select
          className="select-inline"
          value={activeFilter}
          onChange={(event) => {
            setPage(1);
            setActiveFilter(event.target.value as "" | "true" | "false");
          }}
          aria-label="Filter by active state"
        >
          <option value="">All</option>
          <option value="true">Active</option>
          <option value="false">Inactive</option>
        </select>
        <button type="submit" className="btn btn-primary">
          Search
        </button>
        <button type="button" className="btn btn-secondary" onClick={clearFilters}>
          Clear
        </button>
      </form>

      <div className="card">
        <h3 style={{ marginTop: 0 }}>Published announcements</h3>
        {loading && items.length === 0 && <p className="loading-state">Loading…</p>}
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

      <PaginationBar
        page={page}
        totalPages={totalPages}
        total={total}
        limit={limit}
        loading={loading}
        onPageChange={setPage}
        noun="announcements"
      />

      {showComposer && (
        <div className="modal-backdrop" onClick={closeComposer} role="presentation">
          <div
            className="modal-panel modal-announcement"
            onClick={(event) => event.stopPropagation()}
            role="dialog"
            aria-modal="true"
            aria-label="New announcement"
          >
            <div className="modal-header">
              <h2>New announcement</h2>
              <button type="button" className="modal-close" onClick={closeComposer}>
                ×
              </button>
            </div>
            <form className="announcement-composer" onSubmit={handleSubmit}>
              <div className="field">
                <label htmlFor="announcement-title">Title</label>
                <input
                  id="announcement-title"
                  required
                  autoFocus
                  value={title}
                  onChange={(event) => setTitle(event.target.value)}
                />
              </div>
              <div className="field">
                <label htmlFor="announcement-body">Body</label>
                <textarea
                  id="announcement-body"
                  required
                  rows={8}
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
              <div className="announcement-composer-actions">
                <button type="button" className="btn btn-secondary" onClick={closeComposer} disabled={submitting}>
                  Cancel
                </button>
                <button
                  className="btn btn-primary"
                  type="submit"
                  disabled={submitting || !title.trim() || !body.trim()}
                  style={{ width: "auto" }}
                >
                  {submitting ? "Publishing…" : "Publish announcement"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
