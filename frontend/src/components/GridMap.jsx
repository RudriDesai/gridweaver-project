import { useEffect, useState } from "react";
import { MapContainer, TileLayer, Marker, Popup } from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import {
  fetchAllNodes,
  initMockNodes,
  fetchNodeHistory,
} from "../services/api";
import { useWebSocket } from "../hooks/useWebSocket";
import MapLegend from "./MapLegend";

delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl:
    "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png",
  iconUrl:
    "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png",
  shadowUrl:
    "https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png",
});

const STATUS_COLORS = {
  CHARGING: "#22c55e",
  DISCHARGING: "#f97316",
  IDLE: "#3b82f6",
  FAULT: "#ef4444",
};

const STATE_DESCRIPTIONS = {
  CHARGING: "Battery is charging (low grid load)",
  DISCHARGING: "Battery is discharging (high grid load)",
  IDLE: "Battery idle (load within normal range)",
  FAULT: "Battery fault detected",
};

function statusIcon(status, flashing) {
  const color = STATUS_COLORS[status] || "#6b7280";

  const ring = flashing
    ? `box-shadow:0 0 0 4px ${color}55,0 0 6px rgba(0,0,0,.4);`
    : `box-shadow:0 0 4px rgba(0,0,0,.4);`;

  return L.divIcon({
    className: flashing
      ? "custom-node-marker flash"
      : "custom-node-marker",
    html: `
      <div
        style="
          background:${color};
          width:16px;
          height:16px;
          border-radius:50%;
          border:2px solid white;
          ${ring}
        ">
      </div>
    `,
    iconSize: [16, 16],
    iconAnchor: [8, 8],
  });
}

function timeAgo(timestamp) {
  if (!timestamp) return "unknown";

  const seconds = Math.floor((Date.now() - timestamp) / 1000);

  if (seconds < 5) return "just now";
  if (seconds < 60) return `${seconds}s ago`;

  return `${Math.floor(seconds / 60)}m ago`;
}

export default function GridMap() {
  const { connected, lastMessage, reconnectAttempts } = useWebSocket();

  const [nodes, setNodes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Member A
  const [history, setHistory] = useState({});

  // Member B
  const [flashingNodes, setFlashingNodes] = useState(new Set());

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

    if (lastMessage.updateType === "FULL") {
      setNodes(lastMessage.nodes);
      return;
    }

    if (lastMessage.updateType === "PARTIAL") {
      setNodes((prev) => {
        const updated = [...prev];

        lastMessage.nodes.forEach((incoming) => {
          const index = updated.findIndex(
            (node) => node.nodeId === incoming.nodeId
          );

          if (index >= 0) {
            updated[index] = incoming;
          } else {
            updated.push(incoming);
          }
        });

        return updated;
      });

      const ids = lastMessage.nodes.map((n) => n.nodeId);

      setFlashingNodes((prev) => new Set([...prev, ...ids]));

      setTimeout(() => {
        setFlashingNodes((prev) => {
          const next = new Set(prev);

          ids.forEach((id) => next.delete(id));

          return next;
        });
      }, 1200);
    }
  }, [lastMessage]);

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
      </div>

      <div style={{ position: "relative" }}>
        <MapLegend />

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

          {nodes.map((node) => (
            <Marker
              key={node.nodeId}
              position={[node.latitude, node.longitude]}
              icon={statusIcon(
                node.status,
                flashingNodes.has(node.nodeId)
              )}
            >
              <Popup
                eventHandlers={{
                  add: () => loadHistoryFor(node.nodeId),
                }}
              >
                <strong>{node.nodeId}</strong>

                <br />

                Status: {node.status}

                <br />

                <em style={{ color: "#666" }}>
                  {STATE_DESCRIPTIONS[node.status] || ""}
                </em>

                <br />

                Power: {node.powerOutput} kW

                <br />

                Grid Load: {node.gridLoad}%

                <br />

                <span
                  style={{
                    fontSize: "11px",
                    color: "#999",
                  }}
                >
                  Last updated: {timeAgo(node.timestamp)}
                </span>

                {history[node.nodeId] && (
                  <>
                    <br />
                    <span
                      style={{
                        fontSize: "11px",
                        color: "#3b82f6",
                      }}
                    >
                      Last transition:{" "}
                      {history[node.nodeId].fromState} →{" "}
                      {history[node.nodeId].toState}
                    </span>
                  </>
                )}
              </Popup>
            </Marker>
          ))}
        </MapContainer>
      </div>
    </div>
  );
}