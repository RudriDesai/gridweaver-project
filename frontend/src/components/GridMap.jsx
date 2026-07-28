import { useEffect, useRef, useState } from "react";
import { MapContainer, TileLayer } from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import {
  fetchAllNodes,
  initMockNodes,
  fetchNodeHistory,
  fetchLastKafkaEvent,
} from "../services/api";
import { useWebSocket } from "../hooks/useWebSocket";
import MapLegend from "./MapLegend";
import GridNodeMarker from "./GridNodeMarker";
import TransitionToast from "./TransitionToast";
import HeatmapLayer from "./HeatmapLayer";
import PowerTransferArrows from "./PowerTransferArrows";

delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl:
    "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png",
  iconUrl:
    "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png",
  shadowUrl:
    "https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png",
});

export default function GridMap() {
  const { connected, lastMessage, reconnectAttempts } = useWebSocket();

  const [nodes, setNodes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Member A
  const [history, setHistory] = useState({});
  const [kafkaEvents, setKafkaEvents] = useState({});
  // Member B
  const [flashingNodes, setFlashingNodes] = useState(new Set());
  const [heatmapMode, setHeatmapMode] = useState("off");
  const [zoneStats, setZoneStats] = useState([]);
  // Member A — Phase A16
  const [transfers, setTransfers] = useState([]);

  const updateBuffer = useRef([]);
  const flushTimer = useRef(null);

  const [toastEvent, setToastEvent] = useState(null);

  const loadNodes = async () => {
    setLoading(true);
    setError(null);

    try {
      let data = await fetchAllNodes();

      if (data.length === 0) {
        data = await initMockNodes(20);
      }

      setNodes(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  async function triggerStorm() {
    try {
      await fetch("http://localhost:8080/api/simulator/storm?nodeCount=50", { method: "POST" });
    } catch {
      // non-critical for demo purposes
    }
  }
  async function loadHistoryFor(nodeId) {
    try {
      const records = await fetchNodeHistory(nodeId, 1);

      if (records.length > 0) {
        setHistory((prev) => ({
          ...prev,
          [nodeId]: records[0],
        }));
      }
    } catch {
      // ignore history errors
    }
  }

  async function loadKafkaEventFor(nodeId) {
    try {
      const event = await fetchLastKafkaEvent(nodeId);

      setKafkaEvents((prev) => ({
        ...prev,
        [nodeId]: event,
      }));
    } catch {
      // ignore Kafka lookup errors
    }
  }

  useEffect(() => {
    loadNodes();
  }, []);

  useEffect(() => {
    if (
      lastMessage?.type !== "NODE_UPDATE" ||
      !Array.isArray(lastMessage.nodes)
    ) {
      return;
    }

    // FULL sync remains immediate
    if (lastMessage.updateType === "FULL") {
      setNodes(lastMessage.nodes);
      return;
    }

    // Live transition feedback: surface the most recent node in this
    // batch as a toast for a few seconds.
    const latest = lastMessage.nodes[lastMessage.nodes.length - 1];
    if (latest) {
      setToastEvent({ nodeId: latest.nodeId, status: latest.status, ts: Date.now() });
    }

    // Buffer PARTIAL updates
    updateBuffer.current.push(...lastMessage.nodes);

    if (!flushTimer.current) {
      flushTimer.current = setTimeout(() => {
        const incomingBatch = updateBuffer.current;

        updateBuffer.current = [];
        flushTimer.current = null;

        setNodes((prev) => {
          const updated = [...prev];
          const changedIds = [];

          incomingBatch.forEach((incoming) => {
            const index = updated.findIndex(
              (node) => node.nodeId === incoming.nodeId
            );

            if (index >= 0) {
              if (updated[index].status !== incoming.status) {
                changedIds.push(incoming.nodeId);
              }

              updated[index] = incoming;
            } else {
              updated.push(incoming);
              changedIds.push(incoming.nodeId);
            }
          });

          if (changedIds.length > 0) {
            setFlashingNodes((prev) => {
              return new Set([...prev, ...changedIds]);
            });

            setTimeout(() => {
              setFlashingNodes((prev) => {
                const next = new Set(prev);

                changedIds.forEach((id) => next.delete(id));

                return next;
              });
            }, 1500);
          }

          return updated;
        });
      }, 150);
    }
  }, [lastMessage]);
  useEffect(() => {
    if (
      lastMessage?.type === "ZONE_UPDATE" &&
      Array.isArray(lastMessage.zones)
    ) {
      setZoneStats(lastMessage.zones);
    }
  }, [lastMessage]);
  useEffect(() => {
    if (
      lastMessage?.type === "BALANCING_EVENT" &&
      Array.isArray(lastMessage.events)
    ) {
      setTransfers(lastMessage.events);

      // Clear arrows after 6 seconds
      const timer = setTimeout(() => {
        setTransfers([]);
      }, 6000);

      return () => clearTimeout(timer);
    }
  }, [lastMessage]);
  useEffect(() => {
    return () => {
      if (flushTimer.current) {
        clearTimeout(flushTimer.current);
      }
    };
  }, []);
  if (loading) {
    return <div className="status-banner">Loading grid nodes...</div>;
  }

  if (error) {
    return (
      <div className="status-banner error">
        Error: {error}
      </div>
    );
  }

  return (
    <div className="grid-map-wrapper">
      <div className="map-header">
        <h2>GridWeaver — Live Microgrid Map</h2>

        <span>{nodes.length} nodes loaded</span>

        <span
          style={{
            marginLeft: "15px",
            fontWeight: "bold",
          }}
        >
          WebSocket:{" "}
          {connected
            ? "🟢 Connected"
            : `🔴 Reconnecting... (attempt ${reconnectAttempts})`}
        </span>

        <button onClick={loadNodes}>Refresh</button>
        <button onClick={triggerStorm} style={{ marginLeft: "10px", background: "#ef4444", color: "white" }}>
          ⛈ Trigger Storm
        </button>
        <select
          value={heatmapMode}
          onChange={(e) => setHeatmapMode(e.target.value)}
          style={{ marginLeft: "10px" }}
        >
          <option value="off">Heatmap: Off</option>
          <option value="generation">Heatmap: Generation</option>
          <option value="consumption">Heatmap: Consumption</option>
        </select>
      </div>

      <div style={{ position: "relative" }}>
        <MapLegend />
        <TransitionToast event={toastEvent} />

        <MapContainer
          center={[51.505, -0.09]}
          zoom={12}
          style={{
            height: "600px",
            width: "100%",
          }}
        >
          <TileLayer
            attribution="&copy; OpenStreetMap contributors"
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />
          {heatmapMode !== "off" && (
            <HeatmapLayer
              nodes={nodes}
              mode={heatmapMode}
              visible={true}
            />
          )}

          <PowerTransferArrows
            nodes={nodes}
            transfers={transfers}
          />

          {nodes.map((node) => (
            <GridNodeMarker
              key={node.nodeId}
              node={node}
              flashing={flashingNodes.has(node.nodeId)}
              onPopupOpen={(nodeId) => {
                loadHistoryFor(nodeId);
                loadKafkaEventFor(nodeId);
              }}
              lastTransition={history[node.nodeId]}
              lastKafkaEvent={kafkaEvents[node.nodeId]}
            />
          ))}
        </MapContainer>
      </div>
    </div>
  );
}