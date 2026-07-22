import { fetchZoneAnalytics } from "../services/api";
import { usePolling } from "../hooks/usePolling";
import "./RegionalAnalyticsPanel.css";

export default function RegionalAnalyticsPanel() {
  const { data: zones, error } = usePolling(fetchZoneAnalytics, 3000);

  return (
    <div className="analytics-panel">
      <h3>Regional Grid Analytics</h3>
      {error && <p className="status-banner error">Analytics unavailable</p>}
      {!error && (!zones || zones.length === 0) && (
        <p className="analytics-empty">No zone data yet — waiting for telemetry.</p>
      )}
      {zones && zones.length > 0 && (
        <table className="analytics-table">
          <thead>
            <tr>
              <th>Zone</th><th>Nodes</th><th>Gen (kW)</th><th>Cons (kW)</th><th>Util %</th>
            </tr>
          </thead>
          <tbody>
            {zones.map((z) => (
              <tr key={z.zoneId}>
                <td>{z.zoneId}</td>
                <td>{z.nodeCount}</td>
                <td>{z.totalGeneration.toFixed(1)}</td>
                <td>{z.totalConsumption.toFixed(1)}</td>
                <td className={z.utilizationPercent > 80 ? "util-high" : ""}>
                  {z.utilizationPercent.toFixed(1)}%
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}