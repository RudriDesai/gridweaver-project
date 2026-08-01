import { fetchHealth, fetchWsMetrics, fetchAllNodes, fetchProducerMonitoring } from "../services/api";
import { usePolling } from "../hooks/usePolling";
import "./KpiCards.css";

function Kpi({ label, value, tone = "default" }) {
  return (
    <div className="kpi-card">
      <span className="kpi-label">{label}</span>
      <strong className={`kpi-value tone-${tone}`}>{value}</strong>
    </div>
  );
}

export default function KpiCards() {
  const { data: health } = usePolling(fetchHealth, 5000);
  const { data: metrics } = usePolling(fetchWsMetrics, 3000);
  const { data: nodes } = usePolling(fetchAllNodes, 5000);
  const { data: producer } = usePolling(fetchProducerMonitoring, 5000);

  const totalNodes = nodes?.length ?? "—";
  const faultCount = Array.isArray(nodes)
    ? nodes.filter((n) => n.state === "FAULT").length
    : "—";
  const connected = metrics?.activeConnections ?? "—";
  const eventsSent = producer?.totalMessagesSent ?? producer?.totalSent ?? "—";

  return (
    <div className="kpi-grid">
      <Kpi label="Total Nodes" value={totalNodes} />
      <Kpi label="Connected" value={connected} tone="success" />
      <Kpi
        label="Virtual Threads"
        value={health?.virtualThreadsEnabled ? "Enabled" : "Disabled"}
        tone={health?.virtualThreadsEnabled ? "success" : "warning"}
      />
      <Kpi label="Kafka Events" value={eventsSent} />
      <Kpi label="Battery Faults" value={faultCount} tone={faultCount > 0 ? "danger" : "success"} />
      <Kpi
        label="Backend"
        value={health?.status ?? "—"}
        tone={health?.status === "UP" ? "success" : "danger"}
      />
    </div>
  );
}
