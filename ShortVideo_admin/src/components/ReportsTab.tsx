import { FormEvent, useEffect, useState } from "react";
import { ApiError, fetchReports, updateReportStatus } from "../api";
import type { AdminReport } from "../types";
import { PaginationBar } from "./PaginationBar";
import { StatusBadge } from "./StatusBadge";

const STATUS_OPTIONS: Array<AdminReport["status"] | ""> = ["", "OPEN", "RESOLVED", "DISMISSED"];

function reportTitle(report: AdminReport): string {
  if (report.title?.trim()) return report.title.trim();
  const reason = report.reason?.trim() ?? "";
  if (!reason) return "(no title)";
  const parts = reason.split(/\n\n+/);
  if (parts.length >= 2) return parts[0]!.trim() || "(no title)";
  const lines = reason.split("\n");
  return lines[0]!.trim() || "(no title)";
}

function reportMessage(report: AdminReport): string {
  if (report.message?.trim()) return report.message.trim();
  const reason = report.reason?.trim() ?? "";
  const parts = reason.split(/\n\n+/);
  if (parts.length >= 2) return parts.slice(1).join("\n\n").trim();
  const lines = reason.split("\n");
  if (lines.length >= 2) return lines.slice(1).join("\n").trim();
  return "";
}

export function ReportsTab() {
  const [items, setItems] = useState<AdminReport[]>([]);
  const [page, setPage] = useState(1);
  const [total, setTotal] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [limit, setLimit] = useState(20);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<AdminReport["status"] | "">("OPEN");
  const [searchInput, setSearchInput] = useState("");
  const [appliedQ, setAppliedQ] = useState("");
  const [selectedReport, setSelectedReport] = useState<AdminReport | null>(null);

  async function load(
    pageNumber = page,
    filters = {
      status: statusFilter,
      q: appliedQ,
    },
  ) {
    setLoading(true);
    setError(null);
    try {
      const result = await fetchReports(pageNumber, {
        status: filters.status || undefined,
        q: filters.q || undefined,
      });
      setItems(result.items);
      setPage(result.page);
      setTotal(result.total);
      setTotalPages(result.totalPages);
      setLimit(result.limit);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load reports.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load(page, {
      status: statusFilter,
      q: appliedQ,
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, statusFilter, appliedQ]);

  function handleSearch(event: FormEvent) {
    event.preventDefault();
    setPage(1);
    setAppliedQ(searchInput.trim());
  }

  function clearFilters() {
    setSearchInput("");
    setAppliedQ("");
    setStatusFilter("");
    setPage(1);
  }

  async function handleStatusChange(report: AdminReport, status: AdminReport["status"]) {
    try {
      const updated = await updateReportStatus(report.id, status);
      setItems((prev) =>
        prev.map((item) =>
          item.id === report.id ? { ...item, status: updated.status, resolvedAt: updated.resolvedAt } : item,
        ),
      );
      if (selectedReport?.id === report.id) {
        setSelectedReport((prev) =>
          prev ? { ...prev, status: updated.status, resolvedAt: updated.resolvedAt } : prev,
        );
      }
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

      <form className="toolbar toolbar-wrap" onSubmit={handleSearch}>
        <input
          className="input-inline"
          type="search"
          placeholder="Search reporter, title, message, target…"
          value={searchInput}
          onChange={(event) => setSearchInput(event.target.value)}
          aria-label="Search reports"
        />
        <select
          className="select-inline"
          value={statusFilter}
          onChange={(event) => {
            setPage(1);
            setStatusFilter(event.target.value as AdminReport["status"] | "");
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

      <div className="card">
        <table>
          <thead>
            <tr>
              <th>Reporter</th>
              <th>Target</th>
              <th>Title</th>
              <th>Status</th>
              <th>Reported</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            {items.map((report) => (
              <tr
                key={report.id}
                className="clickable-row"
                onClick={() => setSelectedReport(report)}
              >
                <td>{report.reporterName}</td>
                <td>
                  {report.targetType} · <code>{report.targetId}</code>
                </td>
                <td style={{ maxWidth: 320 }}>
                  <button type="button" className="link-btn report-title-btn">
                    {reportTitle(report)}
                  </button>
                </td>
                <td>
                  <StatusBadge status={report.status} />
                </td>
                <td>{new Date(report.createdAt).toLocaleDateString()}</td>
                <td onClick={(event) => event.stopPropagation()}>
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

      <PaginationBar
        page={page}
        totalPages={totalPages}
        total={total}
        limit={limit}
        loading={loading}
        onPageChange={setPage}
        noun="reports"
      />

      {selectedReport && (
        <div
          className="modal-backdrop"
          onClick={() => setSelectedReport(null)}
          role="presentation"
        >
          <div
            className="modal-panel modal-report"
            onClick={(event) => event.stopPropagation()}
            role="dialog"
            aria-modal="true"
            aria-label="Report detail"
          >
            <div className="modal-header">
              <h2>Report detail</h2>
              <button type="button" className="modal-close" onClick={() => setSelectedReport(null)}>
                ×
              </button>
            </div>
            <div className="report-detail">
              <dl className="detail-list detail-list-separated">
                <div>
                  <dt>Title</dt>
                  <dd>{reportTitle(selectedReport)}</dd>
                </div>
                <div>
                  <dt>Message</dt>
                  <dd className="report-message">
                    {reportMessage(selectedReport) || <span className="muted">No message content</span>}
                  </dd>
                </div>
                <div>
                  <dt>Reporter</dt>
                  <dd>{selectedReport.reporterName}</dd>
                </div>
                <div>
                  <dt>Target</dt>
                  <dd>
                    {selectedReport.targetType} · <code>{selectedReport.targetId}</code>
                  </dd>
                </div>
                <div>
                  <dt>Status</dt>
                  <dd>
                    <StatusBadge status={selectedReport.status} />
                  </dd>
                </div>
                <div>
                  <dt>Reported</dt>
                  <dd>{new Date(selectedReport.createdAt).toLocaleString()}</dd>
                </div>
              </dl>
              <div className="report-detail-actions">
                <label htmlFor="report-status-action">Update status</label>
                <select
                  id="report-status-action"
                  className="select-inline"
                  value={selectedReport.status}
                  onChange={(event) =>
                    handleStatusChange(
                      selectedReport,
                      event.target.value as AdminReport["status"],
                    )
                  }
                >
                  <option value="OPEN">OPEN</option>
                  <option value="RESOLVED">RESOLVED</option>
                  <option value="DISMISSED">DISMISSED</option>
                </select>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
