import { memo } from "react";
import { fetchAuditStatistics, getAuditExportUrl } from "../services/api";
import { usePolling } from "../hooks/usePolling";
import "./AuditAnalyticsDashboard.css";

/**
 * Phase B18 — Audit Analytics dashboard: events/hour, state transition
 * breakdown, fault frequency, zone activity, and CSV export.
 * Charts are plain CSS bars — no new chart library dependency.
 */
function AuditAnalyticsDashboard() {
  const { data: stats, error } = usePolling(fetchAuditStatistics, 5000);

  if (error) {
    return (
      <div className="audit-analytics-panel">
        <p className="status-banner error">Audit statistics unavailable</p>
      </div>
    );
  }

  if (!stats) {
    return (
      <div className="audit-analytics-panel">
        <p className="audit-analytics-empty">Loading audit analytics...</p>
      </div>
    );
  }

  const faultRatio = stats.totalEvents > 0 ? (stats.faultTransitionCount / stats.totalEvents) * 100 : 0;
  const faultSeverity = faultRatio >= 20 ? "high" : faultRatio >= 8 ? "medium" : "low";

  const transitions = Object.entries(stats.stateTransitionCounts || {}).sort((a, b) => b[1] - a[1]);
  const zones = Object.entries(stats.zoneActivityCounts || {}).sort((a, b) => b[1] - a[1]);
  const maxTransition = Math.max(1, ...transitions.map(([, c]) => c));
  const maxZone = Math.max(1, ...zones.map(([, c]) => c));

  return (
    <div className="audit-analytics-panel">
      <div className="audit-analytics-header">
        <h3>Audit Analytics</h3>
        <a className="audit-export-btn" href={getAuditExportUrl()} download>
          Export CSV
        </a>
      </div>

      <div className="audit-analytics-summary">
        <div className="audit-summary-card">
          <span className="audit-summary-label">Total Events (1h)</span>
          <span className="audit-summary-value">{stats.totalEvents}</span>
        </div>
        <div className="audit-summary-card">
          <span className="audit-summary-label">Events / Hour</span>
          <span className="audit-summary-value">{stats.eventsPerHour}</span>
        </div>
        <div className="audit-summary-card">
          <span className="audit-summary-label">Fault Frequency</span>
          <span className={`audit-summary-value severity-${faultSeverity}`}>
            {stats.faultTransitionCount} ({faultRatio.toFixed(1)}%)
          </span>
        </div>
      </div>

      <div className="audit-analytics-charts">
        <div className="audit-chart">
          <h4>State Transitions</h4>
          {transitions.length === 0 && <p className="audit-analytics-empty">No transitions yet.</p>}
          {transitions.map(([label, count]) => (
            <div className="audit-bar-row" key={label}>
              <span className="audit-bar-label">{label}</span>
              <div className="audit-bar-track">
                <div className="audit-bar-fill" style={{ width: `${(count / maxTransition) * 100}%` }} />
              </div>
              <span className="audit-bar-count">{count}</span>
            </div>
          ))}
        </div>

        <div className="audit-chart">
          <h4>Zone Activity</h4>
          {zones.length === 0 && <p className="audit-analytics-empty">No zone activity yet.</p>}
          {zones.map(([label, count]) => (
            <div className="audit-bar-row" key={label}>
              <span className="audit-bar-label">{label}</span>
              <div className="audit-bar-track">
                <div className="audit-bar-fill zone" style={{ width: `${(count / maxZone) * 100}%` }} />
              </div>
              <span className="audit-bar-count">{count}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

export default memo(AuditAnalyticsDashboard);