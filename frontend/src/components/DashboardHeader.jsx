import { useEffect, useState } from "react";
import { fetchHealth } from "../services/api";
import { usePolling } from "../hooks/usePolling";
import "./DashboardHeader.css";

export default function DashboardHeader({ darkMode, onToggleDarkMode }) {
  const [now, setNow] = useState(new Date());
  const { data: health } = usePolling(fetchHealth, 5000);

  useEffect(() => {
    const id = setInterval(() => setNow(new Date()), 1000);
    return () => clearInterval(id);
  }, []);

  const connected = health?.status === "UP";

  return (
    <header className="gw-header">
      <div className="gw-header-title">
        <span className="gw-header-icon">⚡</span>
        <div>
          <h1>GridWeaver Dashboard</h1>
          <p>Live Grid Monitoring Platform</p>
        </div>
      </div>

      <div className="gw-header-actions">
        <span className="gw-header-clock">
          Last Update&nbsp;: {now.toLocaleTimeString()}
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
          aria-label="Toggle dark mode"
        >
          {darkMode ? "☀️" : "🌙"}
        </button>
      </div>
    </header>
  );
}
