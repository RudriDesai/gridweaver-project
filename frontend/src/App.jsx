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
import "./App.css";

function App() {
  return (
    <div className="app">
      <header className="app-header">
        <h1>GridWeaver Dashboard</h1>
      </header>
      <div className="app-layout">
        <main className="app-main">
          <GridMap />
          <SimulatorPanel />
          <KafkaProducerStatus />
          <KafkaConsumerStatus />
          <RegionalAnalyticsPanel />
          <EventLog />               {/* Member B — Phase B15 */}
          <AuditAnalyticsDashboard />
          <ProducerMonitoringDashboard />
          <BalancingAnalyticsWidget />
        </main>
        <StatusSidebar />
      </div>
    </div>
  );
}

export default App;