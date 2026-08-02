import { useEffect, useMemo, useState, type CSSProperties } from "react";
import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { ApiError, fetchAnalytics } from "../api";
import type { AdminAnalytics, AnalyticsTrend } from "../types";

const TOTAL_STATS: Array<{ key: keyof AdminAnalytics; label: string }> = [
  { key: "userCount", label: "Total users" },
  { key: "videoCount", label: "Total videos" },
  { key: "readyVideoCount", label: "Published videos" },
  { key: "openReportCount", label: "Open reports" },
  { key: "likeCount", label: "Total likes" },
  { key: "commentCount", label: "Total comments" },
];

const CHART_COLORS = {
  users: "#4f46e5",
  videos: "#0891b2",
  likes: "#dc2626",
  comments: "#d97706",
  reports: "#7c3aed",
  grid: "#e5e7eb",
  axis: "#9ca3af",
};

type RangeDays = 7 | 30;

function formatDay(date: string): string {
  const d = new Date(`${date}T00:00:00Z`);
  return d.toLocaleDateString(undefined, { month: "short", day: "numeric", timeZone: "UTC" });
}

function trendLabel(trend: AnalyticsTrend): string {
  if (trend.changePercent === null) return "n/a";
  const sign = trend.changePercent > 0 ? "+" : "";
  return `${sign}${trend.changePercent}%`;
}

export function AnalyticsTab() {
  const [range, setRange] = useState<RangeDays>(7);
  const [analytics, setAnalytics] = useState<AdminAnalytics | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  async function load(selectedRange: RangeDays = range) {
    setLoading(true);
    setError(null);
    try {
      setAnalytics(await fetchAnalytics(selectedRange));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load analytics.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load(range);
  }, [range]);

  const growthRows = useMemo(() => {
    if (!analytics?.series) return [];
    const { users, videos, likes, comments, reports } = analytics.series;
    return users.map((point, index) => ({
      date: point.date,
      label: formatDay(point.date),
      users: point.count,
      videos: videos[index]?.count ?? 0,
      likes: likes[index]?.count ?? 0,
      comments: comments[index]?.count ?? 0,
      reports: reports[index]?.count ?? 0,
    }));
  }, [analytics]);

  return (
    <div>
      <div className="page-header">
        <div className="page-title-row">
          <h1>Analytics</h1>
          <p className="page-subtitle">
            Lifetime totals plus daily growth for the selected window.
          </p>
        </div>
        <div className="header-actions">
          <div className="range-toggle" role="group" aria-label="Date range">
            <button
              type="button"
              className={`range-btn ${range === 7 ? "active" : ""}`}
              onClick={() => setRange(7)}
              disabled={loading}
            >
              7 days
            </button>
            <button
              type="button"
              className={`range-btn ${range === 30 ? "active" : ""}`}
              onClick={() => setRange(30)}
              disabled={loading}
            >
              30 days
            </button>
          </div>
          <button
            className="btn btn-secondary btn-sm"
            onClick={() => void load(range)}
            disabled={loading}
          >
            Refresh
          </button>
        </div>
      </div>

      {error && <div className="error-banner">{error}</div>}
      {loading && !analytics && <p className="loading-state">Loading…</p>}

      {analytics && (
        <>
          <div className="stat-grid">
            {TOTAL_STATS.map(({ key, label }) => (
              <div className="stat-card" key={key}>
                <div className="stat-label">{label}</div>
                <div className="stat-value">
                  {typeof analytics[key] === "number"
                    ? (analytics[key] as number).toLocaleString()
                    : "—"}
                </div>
              </div>
            ))}
          </div>

          {analytics.trends && (
            <div className="trend-grid">
              {(
                [
                  ["users", "New users", CHART_COLORS.users],
                  ["videos", "New videos", CHART_COLORS.videos],
                  ["likes", "New likes", CHART_COLORS.likes],
                  ["comments", "New comments", CHART_COLORS.comments],
                  ["reports", "New reports", CHART_COLORS.reports],
                ] as const
              ).map(([key, label, color]) => {
                const trend = analytics.trends[key];
                return (
                  <div className="trend-card" key={key}>
                    <div className="trend-card-top">
                      <div className="trend-label">{label}</div>
                      <div className="trend-value">{trend.currentPeriodTotal.toLocaleString()}</div>
                    </div>
                    <div className="trend-meta">
                      <div className={`trend-change ${trend.direction}`} style={{ color }}>
                        <span className="trend-arrow">
                          {trend.direction === "up" ? "▲" : trend.direction === "down" ? "▼" : "●"}
                        </span>
                        {trendLabel(trend)} vs prior {analytics.rangeDays}d
                      </div>
                      <div className="trend-sub">
                        Prior period: {trend.previousPeriodTotal.toLocaleString()}
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}

          <div className="chart-grid">
            <section className="chart-card">
              <div className="chart-header">
                <h2>Growth overview</h2>
                <p>Daily new users and videos</p>
              </div>
              <div className="chart-body">
                <ResponsiveContainer width="100%" height={280}>
                  <AreaChart data={growthRows} margin={{ top: 8, right: 12, left: 0, bottom: 0 }}>
                    <defs>
                      <linearGradient id="usersFill" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="0%" stopColor={CHART_COLORS.users} stopOpacity={0.28} />
                        <stop offset="100%" stopColor={CHART_COLORS.users} stopOpacity={0.02} />
                      </linearGradient>
                      <linearGradient id="videosFill" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="0%" stopColor={CHART_COLORS.videos} stopOpacity={0.24} />
                        <stop offset="100%" stopColor={CHART_COLORS.videos} stopOpacity={0.02} />
                      </linearGradient>
                    </defs>
                    <CartesianGrid stroke={CHART_COLORS.grid} strokeDasharray="3 3" vertical={false} />
                    <XAxis
                      dataKey="label"
                      tick={{ fill: CHART_COLORS.axis, fontSize: 12 }}
                      axisLine={false}
                      tickLine={false}
                      minTickGap={24}
                    />
                    <YAxis
                      allowDecimals={false}
                      tick={{ fill: CHART_COLORS.axis, fontSize: 12 }}
                      axisLine={false}
                      tickLine={false}
                      width={36}
                    />
                    <Tooltip
                      contentStyle={tooltipStyle}
                      labelFormatter={(_, payload) =>
                        payload?.[0]?.payload?.date
                          ? `UTC ${payload[0].payload.date}`
                          : ""
                      }
                    />
                    <Legend />
                    <Area
                      type="monotone"
                      dataKey="users"
                      name="Users"
                      stroke={CHART_COLORS.users}
                      fill="url(#usersFill)"
                      strokeWidth={2}
                      dot={false}
                      activeDot={{ r: 4 }}
                    />
                    <Area
                      type="monotone"
                      dataKey="videos"
                      name="Videos"
                      stroke={CHART_COLORS.videos}
                      fill="url(#videosFill)"
                      strokeWidth={2}
                      dot={false}
                      activeDot={{ r: 4 }}
                    />
                  </AreaChart>
                </ResponsiveContainer>
              </div>
            </section>

            <section className="chart-card">
              <div className="chart-header">
                <h2>Engagement</h2>
                <p>Daily likes and comments</p>
              </div>
              <div className="chart-body">
                <ResponsiveContainer width="100%" height={280}>
                  <LineChart data={growthRows} margin={{ top: 8, right: 12, left: 0, bottom: 0 }}>
                    <CartesianGrid stroke={CHART_COLORS.grid} strokeDasharray="3 3" vertical={false} />
                    <XAxis
                      dataKey="label"
                      tick={{ fill: CHART_COLORS.axis, fontSize: 12 }}
                      axisLine={false}
                      tickLine={false}
                      minTickGap={24}
                    />
                    <YAxis
                      allowDecimals={false}
                      tick={{ fill: CHART_COLORS.axis, fontSize: 12 }}
                      axisLine={false}
                      tickLine={false}
                      width={36}
                    />
                    <Tooltip
                      contentStyle={tooltipStyle}
                      labelFormatter={(_, payload) =>
                        payload?.[0]?.payload?.date
                          ? `UTC ${payload[0].payload.date}`
                          : ""
                      }
                    />
                    <Legend />
                    <Line
                      type="monotone"
                      dataKey="likes"
                      name="Likes"
                      stroke={CHART_COLORS.likes}
                      strokeWidth={2.25}
                      dot={false}
                      activeDot={{ r: 4 }}
                    />
                    <Line
                      type="monotone"
                      dataKey="comments"
                      name="Comments"
                      stroke={CHART_COLORS.comments}
                      strokeWidth={2.25}
                      dot={false}
                      activeDot={{ r: 4 }}
                    />
                  </LineChart>
                </ResponsiveContainer>
              </div>
            </section>

            <section className="chart-card chart-card-wide">
              <div className="chart-header">
                <h2>Moderation load</h2>
                <p>Daily new reports filed</p>
              </div>
              <div className="chart-body">
                <ResponsiveContainer width="100%" height={280}>
                  <BarChart data={growthRows} margin={{ top: 8, right: 12, left: 0, bottom: 0 }}>
                    <CartesianGrid stroke={CHART_COLORS.grid} strokeDasharray="3 3" vertical={false} />
                    <XAxis
                      dataKey="label"
                      tick={{ fill: CHART_COLORS.axis, fontSize: 12 }}
                      axisLine={false}
                      tickLine={false}
                      minTickGap={24}
                    />
                    <YAxis
                      allowDecimals={false}
                      tick={{ fill: CHART_COLORS.axis, fontSize: 12 }}
                      axisLine={false}
                      tickLine={false}
                      width={36}
                    />
                    <Tooltip
                      contentStyle={tooltipStyle}
                      labelFormatter={(_, payload) =>
                        payload?.[0]?.payload?.date
                          ? `UTC ${payload[0].payload.date}`
                          : ""
                      }
                    />
                    <Bar
                      dataKey="reports"
                      name="Reports"
                      fill={CHART_COLORS.reports}
                      radius={[6, 6, 0, 0]}
                      maxBarSize={36}
                    />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </section>
          </div>
        </>
      )}
    </div>
  );
}

const tooltipStyle: CSSProperties = {
  background: "#ffffff",
  border: "1px solid #e5e7eb",
  borderRadius: 10,
  boxShadow: "0 8px 24px rgba(15, 23, 42, 0.08)",
  fontSize: 13,
};
