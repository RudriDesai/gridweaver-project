import { memo } from "react";
import { Marker, Popup } from "react-leaflet";
import L from "leaflet";
import { STATUS_COLORS, STATE_DESCRIPTIONS, timeAgo } from "../mapUtils";

function statusIcon(status, flashing) {
  const color = STATUS_COLORS[status] || "#6b7280";

  const ring = flashing
    ? `box-shadow:0 0 0 4px ${color}55,0 0 6px rgba(0,0,0,.4);`
    : `box-shadow:0 0 4px rgba(0,0,0,.4);`;

  return L.divIcon({
    className: flashing ? "custom-node-marker flash" : "custom-node-marker",
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

function GridNodeMarker({node,flashing,onPopupOpen,lastTransition,lastKafkaEvent}) {
  return (
    <Marker
      position={[node.latitude, node.longitude]}
      icon={statusIcon(node.status, flashing)}
    >
      <Popup
        eventHandlers={{
          add: () => onPopupOpen?.(node.nodeId),
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
        <span style={{ fontSize: "11px", color: "#999" }}>
          Last updated: {timeAgo(node.timestamp)}
        </span>

        {lastTransition && (
          <>
            <br />
            <span style={{ fontSize: "11px", color: "#3b82f6" }}>
              Last transition: {lastTransition.fromState} →{" "}
              {lastTransition.toState}
            </span>
          </>
        )}

        {lastKafkaEvent && (
          <>
            <hr />
            <span style={{ fontSize: "11px", color: "#999" }}>
              Last Kafka Event
            </span>

            <br />
            Zone: {lastKafkaEvent.zoneId}

            <br />
            Battery: {lastKafkaEvent.batteryState}

            <br />
            Generation: {lastKafkaEvent.generation?.toFixed(1)} kW

            <br />
            Consumption: {lastKafkaEvent.consumption?.toFixed(1)} kW
          </>
        )}
      </Popup>
    </Marker>
  );
}

// Only re-render THIS marker when ITS OWN data changed — not on every
// WebSocket tick touching unrelated nodes. Matters once node counts hit
// the thousands (storm scenario).
export default memo(GridNodeMarker, (prev, next) =>
  prev.node.status === next.node.status &&
  prev.node.powerOutput === next.node.powerOutput &&
  prev.node.gridLoad === next.node.gridLoad &&
  prev.node.timestamp === next.node.timestamp &&
  prev.flashing === next.flashing &&
  prev.lastTransition === next.lastTransition &&
  prev.lastKafkaEvent === next.lastKafkaEvent
);