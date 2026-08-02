import { FormEvent, useEffect, useState } from "react";
import { ApiError, fetchUsers, updateUser } from "../api";
import type { AdminUser } from "../types";
import { PaginationBar } from "./PaginationBar";
import { UserAvatar } from "./UserAvatar";

const ROLE_OPTIONS: Array<AdminUser["role"] | ""> = ["", "USER", "ADMIN"];
const STATUS_OPTIONS: Array<AdminUser["status"] | ""> = ["", "ACTIVE", "SUSPENDED", "DELETED"];

export function UsersTab() {
  const [items, setItems] = useState<AdminUser[]>([]);
  const [page, setPage] = useState(1);
  const [total, setTotal] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [limit, setLimit] = useState(20);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [searchInput, setSearchInput] = useState("");
  const [appliedQ, setAppliedQ] = useState("");
  const [roleFilter, setRoleFilter] = useState<AdminUser["role"] | "">("");
  const [statusFilter, setStatusFilter] = useState<AdminUser["status"] | "">("");

  async function load(
    pageNumber = page,
    filters = {
      q: appliedQ,
      role: roleFilter,
      status: statusFilter,
    },
  ) {
    setLoading(true);
    setError(null);
    try {
      const result = await fetchUsers(pageNumber, {
        q: filters.q || undefined,
        role: filters.role || undefined,
        status: filters.status || undefined,
      });
      setItems(result.items);
      setPage(result.page);
      setTotal(result.total);
      setTotalPages(result.totalPages);
      setLimit(result.limit);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load users.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load(page, {
      q: appliedQ,
      role: roleFilter,
      status: statusFilter,
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, appliedQ, roleFilter, statusFilter]);

  function handleSearch(event: FormEvent) {
    event.preventDefault();
    setPage(1);
    setAppliedQ(searchInput.trim());
  }

  function clearFilters() {
    setSearchInput("");
    setAppliedQ("");
    setRoleFilter("");
    setStatusFilter("");
    setPage(1);
  }

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

      <form className="toolbar toolbar-wrap" onSubmit={handleSearch}>
        <input
          className="input-inline"
          type="search"
          placeholder="Search name, @username, email…"
          value={searchInput}
          onChange={(event) => setSearchInput(event.target.value)}
          aria-label="Search users"
        />
        <select
          className="select-inline"
          value={roleFilter}
          onChange={(event) => {
            setPage(1);
            setRoleFilter(event.target.value as AdminUser["role"] | "");
          }}
          aria-label="Filter by role"
        >
          {ROLE_OPTIONS.map((option) => (
            <option key={option || "all-roles"} value={option}>
              {option || "All roles"}
            </option>
          ))}
        </select>
        <select
          className="select-inline"
          value={statusFilter}
          onChange={(event) => {
            setPage(1);
            setStatusFilter(event.target.value as AdminUser["status"] | "");
          }}
          aria-label="Filter by status"
        >
          {STATUS_OPTIONS.map((option) => (
            <option key={option || "all-status"} value={option}>
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

      <div className="card">
        <table>
          <thead>
            <tr>
              <th>User</th>
              <th>Email</th>
              <th>Role</th>
              <th>Status</th>
              <th>Joined</th>
            </tr>
          </thead>
          <tbody>
            {items.map((user) => (
              <tr key={user.id}>
                <td>
                  <div className="user-cell">
                    <UserAvatar
                      name={user.displayName}
                      username={user.username}
                      avatarUrl={user.avatarUrl}
                    />
                    <div className="user-cell-text">
                      <div className="user-cell-name">{user.displayName}</div>
                      <div className="user-cell-subrow">
                        <span className="user-cell-username">@{user.username}</span>
                        <span className="user-cell-bio">
                          {user.bio?.trim() ? user.bio : <span className="muted">No bio</span>}
                        </span>
                      </div>
                    </div>
                  </div>
                </td>
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
                </td>
                <td>{new Date(user.createdAt).toLocaleDateString()}</td>
              </tr>
            ))}
          </tbody>
        </table>

        {loading && <p className="loading-state">Loading…</p>}
        {!loading && items.length === 0 && <p className="empty-state">No users found.</p>}
      </div>

      <PaginationBar
        page={page}
        totalPages={totalPages}
        total={total}
        limit={limit}
        loading={loading}
        onPageChange={setPage}
        noun="users"
      />
    </div>
  );
}
