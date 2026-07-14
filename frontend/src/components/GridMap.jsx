import { useEffect, useState } from "react";
import { MapContainer, TileLayer, Marker, Popup } from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import { fetchAllNodes, initMockNodes } from "../services/api";
import { useWebSocket } from "../hooks/useWebSocket";

delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl:
    "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png",
  iconUrl:
    "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png",
  shadowUrl:
    "https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png",
});

// Color-code markers by node status
const STATUS_COLORS = {
  CHARGING: "#22c55e",
  DISCHARGING: "#f97316",
  IDLE: "#3b82f6",
  FAULT: "#ef4444",
};

// Day 2 (A6) — human-readable description per battery state
const STATE_DESCRIPTIONS = {
  CHARGING: "Battery is charging (low grid load)",
  DISCHARGING: "Battery is discharging (high grid load)",
  IDLE: "Battery idle (load within normal range)",
  FAULT: "Battery fault detected",
};

function statusIcon(status) {
  const color = STATUS_COLORS[status] || "#6b7280";

  return L.divIcon({
    className: "custom-node-marker",
    html: `<div style="
      background:${color};
      width:16px;
      height:16px;
      border-radius:50%;
      border:2px solid white;
      box-shadow:0 0 4px rgba(0,0,0,0.4);
    "></div>`,
    iconSize: [16, 16],
    iconAnchor: [8, 8],
  });
}

export default function GridMap() {
  const { connected, lastMessage } = useWebSocket();

  const [nodes, setNodes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

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

  useEffect(() => {
    loadNodes();
  }, []);

  useEffect(() => {
    if (lastMessage?.type === "NODE_UPDATE" && Array.isArray(lastMessage.nodes)) {
      setNodes(lastMessage.nodes);
    }
  }, [lastMessage]);

  if (loading) {
    return <div className="status-banner">Loading grid nodes...</div>;
  }

  if (error) {
    return <div className="status-banner error">Error: {error}</div>;
  }

  return (
    <div className="grid-map-wrapper">
      <div className="map-header">
        <h2>GridWeaver — Live Microgrid Map</h2>

        <span>{nodes.length} nodes loaded</span>

        <span style={{ marginLeft: "15px", fontWeight: "bold" }}>
          WebSocket: {connected ? "🟢 Connected" : "🔴 Disconnected"}
        </span>

        <button onClick={loadNodes}>Refresh</button>
      </div>

      <MapContainer
        center={[51.505, -0.09]}
        zoom={12}
        style={{ height: "600px", width: "100%" }}
      >
        <TileLayer
          attribution="&copy; OpenStreetMap contributors"
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        {nodes.map((node) => (
          <Marker
            key={node.nodeId}
            position={[node.latitude, node.longitude]}
            icon={statusIcon(node.status)}
          >
            <Popup>
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
            </Popup>
          </Marker>
        ))}
      </MapContainer>
    </div>
  );
}