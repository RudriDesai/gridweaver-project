import { fetchConsumerMonitoring } from "../../services/api";
import { usePolling } from "../../hooks/usePolling";
import "../kafka/ProducerMonitoringDashboard.css"; // reuse Member A's grid/card styles

export default function ConsumerMonitoringDashboard() {
  const { data, error } = usePolling(fetchConsumerMonitoring, 1000);

  return (
    <div className="monitoring-dashboard">
      <h3>Consumer Monitoring</h3>
      {error && <p className="status-banner error">Monitoring unavailable</p>}
      {data && (
        <div className="monitoring-grid">
          <div className="monitoring-card">
            <span className="monitoring-value">{data.processingRatePerSec.toFixed(1)}</span>
            <span className="monitoring-label">Processed/sec</span>
          </div>
          <div className="monitoring-card">
            <span className="monitoring-value">{data.avgProcessingLatencyMs.toFixed(1)}ms</span>
            <span className="monitoring-label">Avg processing time</span>
          </div>
          <div className={`monitoring-card ${data.retryCount > 0 ? "warn" : ""}`}>
            <span className="monitoring-value">{data.retryCount}</span>
            <span className="monitoring-label">Retries</span>
          </div>
          <div className={`monitoring-card ${data.dlqCount > 0 ? "warn" : ""}`}>
            <span className="monitoring-value">{data.dlqCount}</span>
            <span className="monitoring-label">Dead-lettered</span>
          </div>
        </div>
      )}
    </div>
  );
}