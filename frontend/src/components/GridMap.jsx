import { useEffect, useRef, useState } from "react";
import { MapContainer, TileLayer } from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import {
  fetchAllNodes,
  initMockNodes,
  fetchNodeHistory,
} from "../services/api";
import { useWebSocket } from "../hooks/useWebSocket";
import MapLegend from "./MapLegend";
import GridNodeMarker from "./GridNodeMarker";
import TransitionToast from "./TransitionToast";

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

  // Member B
  const [flashingNodes, setFlashingNodes] = useState(new Set());

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

          {nodes.map((node) => (
            <GridNodeMarker
              key={node.nodeId}
              node={node}
              flashing={flashingNodes.has(node.nodeId)}
              onPopupOpen={loadHistoryFor}
              lastTransition={history[node.nodeId]}
            />
          ))}
        </MapContainer>
      </div>
    </div>
  );
}