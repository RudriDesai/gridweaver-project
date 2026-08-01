import { useEffect, useState } from "react";
import DashboardHeader from "./components/DashboardHeader";
import KpiCards from "./components/KpiCards";
import GridMap from "./components/GridMap";
import StatusSidebar from "./components/StatusSidebar";
import SimulatorPanel from "./components/SimulatorPanel";
import KafkaProducerStatus from "./components/kafka/KafkaProducerStatus";
import KafkaConsumerStatus from "./components/kafka/KafkaConsumerStatus";
import RegionalAnalyticsPanel from "./components/RegionalAnalyticsPanel";
import ConsumerMonitoringDashboard from "./components/kafka/ConsumerMonitoringDashboard";
import ProducerMonitoringDashboard from "./components/kafka/ProducerMonitoringDashboard";
import EventLog from "./components/EventLog";
import BalancingAnalyticsWidget from "./components/BalancingAnalyticsWidget";
import AuditAnalyticsDashboard from "./components/AuditAnalyticsDashboard";
import AuditMonitoringPanel from "./components/AuditMonitoringPanel";
import "./App.css";

function App() {
  const [darkMode, setDarkMode] = useState(false);

  useEffect(() => {
    document.documentElement.setAttribute(
      "data-theme",
      darkMode ? "dark" : "light"
    );
  }, [darkMode]);

  return (
    <div className="app">
      <DashboardHeader
        darkMode={darkMode}
        onToggleDarkMode={() => setDarkMode((d) => !d)}
      />

      <KpiCards />

      <div className="gw-grid">
        {/* Row 1 — Map + System status */}
        <div className="gw-col-8 gw-card">
          <GridMap />
        </div>
        <div className="gw-col-4">
          <StatusSidebar />
        </div>

        {/* Row 2 — Regional analytics + Live metrics */}
        <div className="gw-col-6 gw-card">
          <RegionalAnalyticsPanel />
        </div>
        <div className="gw-col-6 gw-card">
          <BalancingAnalyticsWidget />
        </div>

        {/* Row 3 — Event log, full width */}
        <div className="gw-col-12 gw-card">
          <EventLog /> {/* Member B — Phase B15 */}
        </div>

        {/* Row 4 — Kafka producer / consumer */}
        <div className="gw-col-6 gw-card">
          <KafkaProducerStatus />
        </div>
        <div className="gw-col-6 gw-card">
          <KafkaConsumerStatus />
        </div>

        {/* Row 5 — Audit analytics + monitoring */}
        <div className="gw-col-6 gw-card">
          <AuditAnalyticsDashboard />
        </div>
        <div className="gw-col-6 gw-card">
          <AuditMonitoringPanel />
        </div>

        {/* Row 6 — Kafka monitoring dashboards */}
        <div className="gw-col-6 gw-card">
          <ProducerMonitoringDashboard />
        </div>
        <div className="gw-col-6 gw-card">
          <ConsumerMonitoringDashboard />
        </div>

        {/* Row 7 — Simulator, full width */}
        <div className="gw-col-12 gw-card">
          <SimulatorPanel />
        </div>
      </div>
    </div>
  );
}

export default App;
