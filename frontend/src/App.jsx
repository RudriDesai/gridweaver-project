import GridMap from "./components/GridMap";
import StatusSidebar from "./components/StatusSidebar";
import SimulatorPanel from "./components/SimulatorPanel";
import KafkaProducerStatus from "./components/kafka/KafkaProducerStatus";
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
          <KafkaProducerStatus/>
        </main>
        <StatusSidebar />
      </div>
    </div>
  );
}

export default App;