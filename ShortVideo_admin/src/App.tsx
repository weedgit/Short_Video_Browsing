import { useState } from "react";
import { getAccessToken, getStoredUser, setSession } from "./api";
import { AnalyticsTab } from "./components/AnalyticsTab";
import { AnnouncementsTab } from "./components/AnnouncementsTab";
import { LoginForm } from "./components/LoginForm";
import { ReportsTab } from "./components/ReportsTab";
import { UsersTab } from "./components/UsersTab";
import { VideosTab } from "./components/VideosTab";
import type { AuthSession, AuthUser } from "./types";

type TabId = "users" | "videos" | "reports" | "announcements" | "analytics";

const TABS: Array<{ id: TabId; label: string }> = [
  { id: "analytics", label: "Analytics" },
  { id: "users", label: "Users" },
  { id: "videos", label: "Videos" },
  { id: "reports", label: "Reports" },
  { id: "announcements", label: "Announcements" },
];

export default function App() {
  const [user, setUser] = useState<AuthUser | null>(() =>
    getAccessToken() ? getStoredUser() : null,
  );
  const [activeTab, setActiveTab] = useState<TabId>("analytics");

  if (!user) {
    return (
      <LoginForm
        onLoggedIn={(session: AuthSession) => {
          setUser(session.user);
        }}
      />
    );
  }

  function handleLogout() {
    setSession(null);
    setUser(null);
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <h2>ShortVideo</h2>
        <p className="sidebar-subtitle">Admin console</p>

        <nav className="sidebar-nav">
          {TABS.map((tab) => (
            <button
              key={tab.id}
              className={tab.id === activeTab ? "active" : ""}
              onClick={() => setActiveTab(tab.id)}
            >
              {tab.label}
            </button>
          ))}
        </nav>

        <div className="sidebar-footer">
          <div className="who">{user.displayName}</div>
          <button className="btn btn-secondary btn-sm" onClick={handleLogout} style={{ width: "100%" }}>
            Sign out
          </button>
        </div>
      </aside>

      <main className="main-content">
        {activeTab === "analytics" && <AnalyticsTab />}
        {activeTab === "users" && <UsersTab />}
        {activeTab === "videos" && <VideosTab />}
        {activeTab === "reports" && <ReportsTab />}
        {activeTab === "announcements" && <AnnouncementsTab />}
      </main>
    </div>
  );
}
