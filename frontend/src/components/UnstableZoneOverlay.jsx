import { Circle, Tooltip } from "react-leaflet";
import { computeZoneCentroids } from "../zoneUtils";
import { fetchStabilityStatus } from "../services/api";
import { usePolling } from "../hooks/usePolling";
import { memo, useMemo } from "react";

const SEVERITY_STYLE = {
  HIGH: { color: "#dc2626", radius: 1400 },
  MEDIUM: { color: "#d97706", radius: 1100 },
  LOW: { color: "#eab308", radius: 900 },
};

/**
 * Phase A19 — Same centroid-memoization optimization as PowerTransferArrows.
 * Also short-circuits before computing centroids at all when there's
 * nothing unstable to show.
 */
function UnstableZoneOverlay({ nodes }) {
  const { data: statuses } = usePolling(fetchStabilityStatus, 4000);

  const unstable = useMemo(
    () => (statuses ? statuses.filter((s) => !s.stable) : []),
    [statuses]
  );

  const centroids = useMemo(
    () => (unstable.length > 0 ? computeZoneCentroids(nodes) : {}),
    [nodes, unstable.length]
  );

  if (unstable.length === 0) return null;

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

export default memo(UnstableZoneOverlay);