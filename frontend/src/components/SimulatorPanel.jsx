import { useState } from "react";
import { startSimulator, fetchSimulatorStatus } from "../services/api";
import { usePolling } from "../hooks/usePolling";

export default function SimulatorPanel() {
  const [nodeCount, setNodeCount] = useState(500);
  const [messagesPerNode, setMessagesPerNode] = useState(3);
  const [starting, setStarting] = useState(false);
  const [startError, setStartError] = useState(null);

  const { data: status, error: pollError } = usePolling(fetchSimulatorStatus, 1500);

  const handleStart = async () => {
    setStarting(true);
    setStartError(null);
    try {
      await startSimulator(nodeCount, messagesPerNode);
    } catch (err) {
      setStartError(err.message);
    } finally {
      setStarting(false);
    }
  };

  const isRunning = status?.running ?? false;

  return (
    <section className="simulator-panel">
      <h3>IoT Simulator</h3>

      <div className="simulator-controls">
        <label>
          Node count
          <input
            type="number"
            min="1"
            max="50000"
            value={nodeCount}
            onChange={(e) => setNodeCount(Number(e.target.value))}
            disabled={isRunning}
          />
        </label>
        <label>
          Messages/node
          <input
            type="number"
            min="1"
            max="100"
            value={messagesPerNode}
            onChange={(e) => setMessagesPerNode(Number(e.target.value))}
            disabled={isRunning}
          />
        </label>
        <button onClick={handleStart} disabled={isRunning || starting}>
          {isRunning ? "Running…" : starting ? "Starting…" : "Start Simulation"}
        </button>
      </div>

      {startError && <p className="status-banner error">{startError}</p>}
      {pollError && <p className="status-banner error">Status poll failed</p>}

      {status && (
        <ul className="status-list">
          <li><span>Target</span><strong>{status.targetNodeCount}</strong></li>
          <li><span>Connected</span><strong>{status.connected}</strong></li>
          <li><span>Failed</span><strong>{status.failed}</strong></li>
          <li><span>ACKs</span><strong>{status.acksReceived}</strong></li>
          <li><span>Elapsed</span><strong>{status.elapsedMs} ms</strong></li>
        </ul>
      )}
    </section>
  );
}