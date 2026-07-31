import { useEffect, useState } from "react";
import { ApiError, fetchAnalytics } from "../api";
import type { AdminAnalytics } from "../types";

const STAT_LABELS: Array<{ key: keyof AdminAnalytics; label: string }> = [
  { key: "userCount", label: "Total users" },
  { key: "videoCount", label: "Total videos" },
  { key: "readyVideoCount", label: "Published videos" },
  { key: "openReportCount", label: "Open reports" },
  { key: "likeCount", label: "Total likes" },
  { key: "commentCount", label: "Total comments" },
];

export function AnalyticsTab() {
  const [analytics, setAnalytics] = useState<AdminAnalytics | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  async function load() {
    setLoading(true);
    setError(null);
    try {
      setAnalytics(await fetchAnalytics());
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load analytics.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load();
  }, []);

  return (
    <div>
      <div className="page-header">
        <h1>Analytics</h1>
        <button className="btn btn-secondary btn-sm" onClick={() => void load()} disabled={loading}>
          Refresh
        </button>
      </div>

      {error && <div className="error-banner">{error}</div>}
      {loading && !analytics && <p className="loading-state">Loading…</p>}

      {analytics && (
        <div className="stat-grid">
          {STAT_LABELS.map(({ key, label }) => (
            <div className="stat-card" key={key}>
              <div className="stat-label">{label}</div>
              <div className="stat-value">{analytics[key].toLocaleString()}</div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
