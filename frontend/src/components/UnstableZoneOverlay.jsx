import { Circle, Tooltip } from "react-leaflet";
import { computeZoneCentroids } from "../zoneUtils";
import { fetchStabilityStatus } from "../services/api";
import { usePolling } from "../hooks/usePolling";

const SEVERITY_STYLE = {
  HIGH: { color: "#dc2626", radius: 1400 },
  MEDIUM: { color: "#d97706", radius: 1100 },
  LOW: { color: "#eab308", radius: 900 },
};

/**
 * Phase A18 — Highlights unstable zones (overloaded or fault-clustered)
 * as a pulsing colored circle centered on each zone's centroid. Polls the
 * live status endpoint so highlighting persists even between alert events.
 */
export default function UnstableZoneOverlay({ nodes }) {
  const { data: statuses } = usePolling(fetchStabilityStatus, 4000);

  if (!statuses || statuses.length === 0 || !nodes || nodes.length === 0) return null;

  const centroids = computeZoneCentroids(nodes);
  const unstable = statuses.filter((s) => !s.stable);

  return (
    <>
      {unstable.map((s) => {
        const center = centroids[s.zoneId];
        if (!center) return null;
        const style = SEVERITY_STYLE[s.severity] || SEVERITY_STYLE.LOW;

        return (
          <Circle
            key={s.zoneId}
            center={center}
            radius={style.radius}
            pathOptions={{ color: style.color, fillColor: style.color, fillOpacity: 0.15, weight: 2 }}
          >
            <Tooltip direction="top" permanent={false}>
              {s.zoneId}: {s.utilizationPercent.toFixed(0)}% load, {s.faultNodeCount} fault(s)
            </Tooltip>
          </Circle>
        );
      })}
    </>
  );
}