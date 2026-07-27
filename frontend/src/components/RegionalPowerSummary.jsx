import { memo } from "react";
import { fetchZoneAnalytics, fetchBalancingRecommendations } from "../services/api";
import { usePolling } from "../hooks/usePolling";
import "./RegionalPowerSummary.css";

/**
 * Phase A15 — Regional Power Summary cards.
 * Shows per-zone generation / consumption / utilization, plus any
 * surplus -> deficit balancing recommendations computed server-side.
 */
function RegionalPowerSummary() {
  const { data: zones, error: zonesError } = usePolling(fetchZoneAnalytics, 3000);
  const { data: recommendations } = usePolling(fetchBalancingRecommendations, 3000);

  return (
    <div className="power-summary-panel">
      <h3>Regional Power Summary</h3>

      {zonesError && <p className="status-banner error">Zone data unavailable</p>}

      {!zonesError && (!zones || zones.length === 0) && (
        <p className="power-summary-empty">No zone data yet — waiting for telemetry.</p>
      )}

      {zones && zones.length > 0 && (
        <div className="power-summary-cards">
          {zones.map((z) => {
            const net = z.totalGeneration - z.totalConsumption;
            return (
              <div key={z.zoneId} className="power-summary-card">
                <div className="card-header">
                  <span className="zone-name">{z.zoneId}</span>
                  <span className={`net-badge ${net >= 0 ? "surplus" : "deficit"}`}>
                    {net >= 0 ? "Surplus" : "Deficit"}
                  </span>
                </div>
                <div className="card-row"><span>Generation</span><strong>{z.totalGeneration.toFixed(1)} kW</strong></div>
                <div className="card-row"><span>Consumption</span><strong>{z.totalConsumption.toFixed(1)} kW</strong></div>
                <div className="card-row"><span>Utilization</span><strong>{z.utilizationPercent.toFixed(1)}%</strong></div>
                <div className="card-row"><span>Nodes</span><strong>{z.nodeCount}</strong></div>
              </div>
            );
          })}
        </div>
      )}

      {recommendations && recommendations.length > 0 && (
        <div className="balancing-recommendations">
          <h4>Balancing Recommendations</h4>
          <ul>
            {recommendations.map((r, i) => (
              <li key={i} className={`rec-${r.severity.toLowerCase()}`}>
                Transfer <strong>{r.recommendedTransfer} kW</strong> from{" "}
                <strong>{r.fromZone}</strong> → <strong>{r.toZone}</strong>{" "}
                <span className="rec-severity">({r.severity})</span>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}

export default memo(RegionalPowerSummary);