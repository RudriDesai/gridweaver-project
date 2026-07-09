import { fetchHealth, fetchWsMetrics } from "../services/api";
import { usePolling } from "../hooks/usePolling";

export default function StatusSidebar() {
  const { data: health, error: healthError } = usePolling(fetchHealth, 5000);
  const { data: metrics, error: metricsError } = usePolling(fetchWsMetrics, 2000);

  return (
    <aside className="status-sidebar">
      <h3>Backend Status</h3>

      {healthError && <p className="status-banner error">Backend unreachable</p>}

      {health && (
        <ul className="status-list">
          <li>
            <span>Status</span>
            <strong className={health.status === "UP" ? "ok" : "bad"}>
              {health.status}
            </strong>
          </li>
          <li>
            <span>Virtual Threads</span>
            <strong>{health.virtualThreadsEnabled ? "Enabled" : "Disabled"}</strong>
          </li>
        </ul>
      )}

      <h3>Live WebSocket Metrics</h3>
      {metricsError && <p className="status-banner error">Metrics unavailable</p>}
      {metrics && (
        <ul className="status-list">
          <li><span>Active Connections</span><strong>{metrics.activeConnections}</strong></li>
          <li><span>Total Connections</span><strong>{metrics.totalConnectionsEver}</strong></li>
          <li><span>Messages Received</span><strong>{metrics.totalMessagesReceived}</strong></li>
        </ul>
      )}
    </aside>
  );
}