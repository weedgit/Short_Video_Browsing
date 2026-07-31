import { useEffect, useState } from "react";
import { ApiError, fetchReports, updateReportStatus } from "../api";
import type { AdminReport } from "../types";
import { StatusBadge } from "./StatusBadge";

const STATUS_OPTIONS: Array<AdminReport["status"] | ""> = ["", "OPEN", "RESOLVED", "DISMISSED"];

export function ReportsTab() {
  const [items, setItems] = useState<AdminReport[]>([]);
  const [cursor, setCursor] = useState<string | null>(null);
  const [hasMore, setHasMore] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<AdminReport["status"] | "">("OPEN");

  async function load(nextCursor?: string, filter = statusFilter) {
    setLoading(true);
    setError(null);
    try {
      const page = await fetchReports(nextCursor, filter || undefined);
      setItems((prev) => (nextCursor ? [...prev, ...page.items] : page.items));
      setCursor(page.nextCursor);
      setHasMore(page.hasMore);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load reports.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load(undefined, statusFilter);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [statusFilter]);

  async function handleStatusChange(report: AdminReport, status: AdminReport["status"]) {
    try {
      const updated = await updateReportStatus(report.id, status);
      setItems((prev) =>
        prev.map((item) =>
          item.id === report.id ? { ...item, status: updated.status, resolvedAt: updated.resolvedAt } : item,
        ),
      );
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to update report.");
    }
  }

  return (
    <div>
      <div className="page-header">
        <h1>Reports</h1>
      </div>

      {error && <div className="error-banner">{error}</div>}

      <div className="toolbar">
        <label htmlFor="report-filter" style={{ fontSize: 13, color: "var(--text-muted)" }}>
          Filter by status
        </label>
        <select
          id="report-filter"
          className="select-inline"
          value={statusFilter}
          onChange={(event) => setStatusFilter(event.target.value as AdminReport["status"] | "")}
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
              <th>Reporter</th>
              <th>Target</th>
              <th>Reason</th>
              <th>Status</th>
              <th>Reported</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            {items.map((report) => (
              <tr key={report.id}>
                <td>{report.reporterName}</td>
                <td>
                  {report.targetType} · <code>{report.targetId}</code>
                </td>
                <td style={{ maxWidth: 280 }}>{report.reason}</td>
                <td>
                  <StatusBadge status={report.status} />
                </td>
                <td>{new Date(report.createdAt).toLocaleDateString()}</td>
                <td>
                  <select
                    className="select-inline"
                    value={report.status}
                    onChange={(event) =>
                      handleStatusChange(report, event.target.value as AdminReport["status"])
                    }
                  >
                    <option value="OPEN">OPEN</option>
                    <option value="RESOLVED">RESOLVED</option>
                    <option value="DISMISSED">DISMISSED</option>
                  </select>
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        {loading && <p className="loading-state">Loading…</p>}
        {!loading && items.length === 0 && <p className="empty-state">No reports found.</p>}
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
