import { memo } from "react";
import { fetchBalancingMetrics } from "../services/api";
import { usePolling } from "../hooks/usePolling";
import "./BalancingAnalyticsWidget.css";

/**
 * Phase A17 — Live analytics widgets for regional balancing:
 * surplus/deficit gauge, transferred energy, and efficiency score.
 */
function BalancingAnalyticsWidget() {
  const { data: metrics, error } = usePolling(fetchBalancingMetrics, 3000);

  if (error) {
    return (
      <div className="balancing-analytics-panel">
        <p className="status-banner error">Balancing metrics unavailable</p>
      </div>
    );
  }

  if (!metrics) {
    return (
      <div className="balancing-analytics-panel">
        <p className="balancing-analytics-empty">Loading balancing analytics...</p>
      </div>
    );
  }

  const efficiencyClass =
    metrics.balancingEfficiencyPercent >= 80 ? "good" :
    metrics.balancingEfficiencyPercent >= 50 ? "warn" : "bad";

  return (
    <div className="balancing-analytics-panel">
      <h3>Grid Balancing Analytics</h3>

      <div className="analytics-grid">
        <div className="analytics-card">
          <span className="analytics-label">Total Surplus</span>
          <span className="analytics-value surplus">{metrics.totalSurplusKw} kW</span>
          <span className="analytics-sub">{metrics.activeSurplusZones} zone(s)</span>
        </div>

        <div className="analytics-card">
          <span className="analytics-label">Total Deficit</span>
          <span className="analytics-value deficit">{metrics.totalDeficitKw} kW</span>
          <span className="analytics-sub">{metrics.activeDeficitZones} zone(s)</span>
        </div>

        <div className="analytics-card">
          <span className="analytics-label">Transferred (5 min)</span>
          <span className="analytics-value transferred">{metrics.totalTransferredKw} kW</span>
          <span className="analytics-sub">{metrics.executedTransferCount} transfer(s)</span>
        </div>

        <div className="analytics-card">
          <span className="analytics-label">Balancing Efficiency</span>
          <span className={`analytics-value efficiency ${efficiencyClass}`}>
            {metrics.balancingEfficiencyPercent}%
          </span>
          <div className="efficiency-bar">
            <div
              className={`efficiency-bar-fill ${efficiencyClass}`}
              style={{ width: `${metrics.balancingEfficiencyPercent}%` }}
            />
          </div>
        </div>
      </div>
    </div>
  );
}

export default memo(BalancingAnalyticsWidget);