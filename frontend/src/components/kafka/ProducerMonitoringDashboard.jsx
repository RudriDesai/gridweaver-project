import { fetchProducerMonitoring } from "../../services/api";
import { usePolling } from "../../hooks/usePolling";
import "./ProducerMonitoringDashboard.css";

export default function ProducerMonitoringDashboard() {
  const { data, error } = usePolling(fetchProducerMonitoring, 1000);

  return (
    <div className="monitoring-dashboard">
      <h3>Producer Monitoring</h3>
      {error && <p className="status-banner error">Monitoring unavailable</p>}
      {data && (
        <div className="monitoring-grid">
          <div className="monitoring-card">
            <span className="monitoring-value">{data.publishRatePerSec.toFixed(1)}</span>
            <span className="monitoring-label">Publish rate/sec</span>
          </div>
          <div className="monitoring-card">
            <span className="monitoring-value">{data.queueSize}</span>
            <span className="monitoring-label">In-flight queue</span>
          </div>
          <div className={`monitoring-card ${data.failedPublishes > 0 ? "warn" : ""}`}>
            <span className="monitoring-value">{data.failedPublishes}</span>
            <span className="monitoring-label">Failed publishes</span>
          </div>
          <div className="monitoring-card">
            <span className="monitoring-value">{data.avgLatencyMs.toFixed(1)}ms</span>
            <span className="monitoring-label">Avg latency</span>
          </div>
        </div>
      )}
    </div>
  );
}