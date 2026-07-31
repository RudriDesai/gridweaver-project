import { memo, useEffect, useRef, useState } from "react";
import { fetchAuditHealth } from "../services/api";
import { usePolling } from "../hooks/usePolling";
import { useWebSocket } from "../hooks/useWebSocket";
import "./AuditMonitoringPanel.css";

/**
 * Phase B19 — Final integration widget: total events, live event rate,
 * WebSocket connection status, and pipeline health, all in one panel.
 */
function AuditMonitoringPanel() {
  const { data: health, error } = usePolling(fetchAuditHealth, 3000);
  const { connected, lastMessage } = useWebSocket();

  // Client-observed live rate: counts events arriving via WS over a
  // rolling 10s window, as a real-time cross-check against the server's
  // eventsPerHourRecent (which is a longer 1h window).
  const recentTimestamps = useRef([]);
  const [liveRatePerMin, setLiveRatePerMin] = useState(0);

  useEffect(() => {
    if (!lastMessage) return;
    let count = 0;
    if (lastMessage.type === "AUDIT_EVENT_BATCH") count = lastMessage.events?.length || 0;
    else if (lastMessage.type === "AUDIT_EVENT") count = 1;
    if (count === 0) return;

    const now = Date.now();
    const arr = recentTimestamps.current;
    for (let i = 0; i < count; i++) arr.push(now);

    const cutoff = now - 10000;
    recentTimestamps.current = arr.filter((t) => t >= cutoff);
    setLiveRatePerMin(Math.round((recentTimestamps.current.length / 10) * 60));
  }, [lastMessage]);

  if (error) {
    return (
      <div className="audit-monitoring-panel">
        <p className="status-banner error">Audit health unavailable</p>
      </div>
    );
  }

  return (
    <div className="audit-monitoring-panel">
      <h3>Audit Monitoring</h3>

      <div className="monitoring-grid">
        <div className="monitoring-card">
          <span className="monitoring-label">Total Events Stored</span>
          <span className="monitoring-value">{health?.totalEventsStored ?? "—"}</span>
        </div>

        <div className="monitoring-card">
          <span className="monitoring-label">Live Event Rate</span>
          <span className="monitoring-value">{liveRatePerMin}/min</span>
          <span className="monitoring-sub">
            {health ? `${health.eventsPerHourRecent}/hr (1h avg)` : ""}
          </span>
        </div>

        <div className="monitoring-card">
          <span className="monitoring-label">WebSocket Status</span>
          <span className={`monitoring-value ws-status ${connected ? "up" : "down"}`}>
            <span className="ws-dot" /> {connected ? "Connected" : "Disconnected"}
          </span>
          <span className="monitoring-sub">
            {health ? `${health.activeWebSocketConnections} active connection(s)` : ""}
          </span>
        </div>

        <div className="monitoring-card">
          <span className="monitoring-label">Pipeline Health</span>
          <span className={`monitoring-value pipeline-status ${health?.status === "UP" ? "up" : "down"}`}>
            {health?.status ?? "—"}
          </span>
          <span className="monitoring-sub">
            {health ? `${health.pendingBroadcastQueueSize} queued` : ""}
          </span>
        </div>
      </div>
    </div>
  );
}

export default memo(AuditMonitoringPanel);