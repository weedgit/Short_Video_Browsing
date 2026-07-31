import { useEffect, useState } from "react";
import { ApiError, fetchUsers, updateUser } from "../api";
import type { AdminUser } from "../types";
import { StatusBadge } from "./StatusBadge";

export function UsersTab() {
  const [items, setItems] = useState<AdminUser[]>([]);
  const [cursor, setCursor] = useState<string | null>(null);
  const [hasMore, setHasMore] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  async function load(nextCursor?: string) {
    setLoading(true);
    setError(null);
    try {
      const page = await fetchUsers(nextCursor);
      setItems((prev) => (nextCursor ? [...prev, ...page.items] : page.items));
      setCursor(page.nextCursor);
      setHasMore(page.hasMore);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load users.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function handleStatusChange(user: AdminUser, status: AdminUser["status"]) {
    try {
      const updated = await updateUser(user.id, { status });
      setItems((prev) => prev.map((item) => (item.id === user.id ? updated : item)));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to update user.");
    }
  }

  async function handleRoleChange(user: AdminUser, role: AdminUser["role"]) {
    try {
      const updated = await updateUser(user.id, { role });
      setItems((prev) => prev.map((item) => (item.id === user.id ? updated : item)));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to update user.");
    }
  }

  return (
    <div>
      <div className="page-header">
        <h1>Users</h1>
      </div>

      {error && <div className="error-banner">{error}</div>}

      <div className="card">
        <table>
          <thead>
            <tr>
              <th>Display name</th>
              <th>Username</th>
              <th>Email</th>
              <th>Role</th>
              <th>Status</th>
              <th>Joined</th>
            </tr>
          </thead>
          <tbody>
            {items.map((user) => (
              <tr key={user.id}>
                <td>{user.displayName}</td>
                <td>@{user.username}</td>
                <td>{user.email}</td>
                <td>
                  <select
                    className="select-inline"
                    value={user.role}
                    onChange={(event) => handleRoleChange(user, event.target.value as AdminUser["role"])}
                  >
                    <option value="USER">USER</option>
                    <option value="ADMIN">ADMIN</option>
                  </select>
                </td>
                <td>
                  <select
                    className="select-inline"
                    value={user.status}
                    onChange={(event) =>
                      handleStatusChange(user, event.target.value as AdminUser["status"])
                    }
                  >
                    <option value="ACTIVE">ACTIVE</option>
                    <option value="SUSPENDED">SUSPENDED</option>
                    <option value="DELETED">DELETED</option>
                  </select>
                  <div style={{ marginTop: 4 }}>
                    <StatusBadge status={user.status} />
                  </div>
                </td>
                <td>{new Date(user.createdAt).toLocaleDateString()}</td>
              </tr>
            ))}
          </tbody>
        </table>

        {loading && <p className="loading-state">Loading…</p>}
        {!loading && items.length === 0 && <p className="empty-state">No users found.</p>}
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
