import { Polyline, Marker } from "react-leaflet";
import L from "leaflet";
import { computeZoneCentroids, bearingDegrees, SEVERITY_COLORS } from "../zoneUtils";

/**
 * Phase A16 — Draws a dashed line + rotated arrowhead marker between
 * zone centroids for each active balancing transfer. No new dependency:
 * arrowhead is a plain divIcon rotated with CSS transform.
 */
function arrowIcon(angleDeg, color) {
  return L.divIcon({
    className: "power-transfer-arrow-icon",
    html: `<div style="
        width:0;height:0;
        border-left:6px solid transparent;
        border-right:6px solid transparent;
        border-bottom:12px solid ${color};
        transform: rotate(${angleDeg}deg);
        transform-origin: center;
      "></div>`,
    iconSize: [12, 12],
    iconAnchor: [6, 6],
  });
}

export default function PowerTransferArrows({ nodes, transfers }) {
  if (!transfers || transfers.length === 0) return null;

  const centroids = computeZoneCentroids(nodes);

  return (
    <>
      {transfers.map((t) => {
        const from = centroids[t.fromZone];
        const to = centroids[t.toZone];
        if (!from || !to) return null;

        const color = SEVERITY_COLORS[t.severity] || "#3b82f6";
        const midpoint = [(from[0] + to[0]) / 2, (from[1] + to[1]) / 2];
        // Bearing math points north(0deg); the CSS triangle points down by
        // default, so we offset by 180deg to align the tip with travel direction.
        const angle = bearingDegrees(from, to) + 180;

        return (
          <div key={t.eventId}>
            <Polyline
              positions={[from, to]}
              pathOptions={{ color, weight: 3, dashArray: "6 6", opacity: 0.85 }}
            />
            <Marker position={midpoint} icon={arrowIcon(angle, color)} />
          </div>
        );
      })}
    </>
  );
}