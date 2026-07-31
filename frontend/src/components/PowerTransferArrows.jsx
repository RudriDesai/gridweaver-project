import { Polyline, Marker } from "react-leaflet";
import L from "leaflet";
import { memo, useMemo } from "react";
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

/**
 * Phase A19 — Centroid computation (a full pass over `nodes`) is now
 * memoized on `nodes` so it only recomputes when the node list actually
 * changes, not on every parent re-render triggered by unrelated state
 * (e.g. sidebar polling). Wrapped in memo() with a shallow prop check.
 */
function PowerTransferArrows({ nodes, transfers }) {
  const centroids = useMemo(() => computeZoneCentroids(nodes), [nodes]);

  if (!transfers || transfers.length === 0) return null;

  return (
    <>
      {transfers.map((t) => {
        const from = centroids[t.fromZone];
        const to = centroids[t.toZone];
        if (!from || !to) return null;

        const color = SEVERITY_COLORS[t.severity] || "#3b82f6";
        const midpoint = [(from[0] + to[0]) / 2, (from[1] + to[1]) / 2];
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

export default memo(PowerTransferArrows);