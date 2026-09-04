import { useEffect, useState } from "react";
import { fetchHealth } from "../services/api";
import { usePolling } from "../hooks/usePolling";
import "./DashboardHeader.css";

export default function DashboardHeader({ darkMode, onToggleDarkMode, onNavigateHome }) {
  const [now, setNow] = useState(new Date());
  const { data: health } = usePolling(fetchHealth, 5000);

  useEffect(() => {
    const id = setInterval(() => setNow(new Date()), 1000);
    return () => clearInterval(id);
  }, []);

  const connected = health?.status === "UP";

  return (
    <header className="gw-header">
      <button
        type="button"
        className="gw-header-title"
        onClick={onNavigateHome}
        aria-label="GridWeaver — back to home page"
      >
        <span className="gw-header-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" width="26" height="26" fill="none">
            <path
              d="M13 2 4 14h6l-1 8 9-12h-6l1-8Z"
              fill="currentColor"
            />
          </svg>
        </span>
        <span className="gw-header-title-text">
          <h1>GridWeaver</h1>
          <p>Live Grid Monitoring Platform</p>
        </span>
      </button>

      <div className="gw-header-actions">
        <span className="gw-header-clock">
          Last update&nbsp;: {now.toLocaleTimeString()}
        </span>

        <span className={`gw-badge ${connected ? "ok" : "bad"}`}>
          <span className="dot" />
          {connected ? "Connected" : "Offline"}
        </span>

        <button className="gw-btn" type="button">
          Storm Mode
        </button>

        <button
          className="gw-btn gw-btn-icon"
          type="button"
          onClick={onToggleDarkMode}
          aria-label="Toggle light and dark mode"
        >
          {darkMode ? "☀️" : "🌙"}
        </button>
      </div>
    </header>
  );
}
