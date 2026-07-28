/**
 * Phase A16 — Computes the geographic centroid of each zone from the
 * live node list, so zone-to-zone transfers can be drawn as arrows
 * without needing separately-maintained zone coordinates.
 */
export function computeZoneCentroids(nodes) {
  const groups = {};

  nodes.forEach((n) => {
    if (!n.zoneId) return;
    if (!groups[n.zoneId]) groups[n.zoneId] = { latSum: 0, lngSum: 0, count: 0 };
    groups[n.zoneId].latSum += n.latitude;
    groups[n.zoneId].lngSum += n.longitude;
    groups[n.zoneId].count += 1;
  });

  const centroids = {};
  Object.entries(groups).forEach(([zoneId, g]) => {
    centroids[zoneId] = [g.latSum / g.count, g.lngSum / g.count];
  });

  return centroids;
}

/** Bearing in degrees from point A to point B, for rotating arrowheads. */
export function bearingDegrees([lat1, lng1], [lat2, lng2]) {
  const toRad = (d) => (d * Math.PI) / 180;
  const toDeg = (r) => (r * 180) / Math.PI;

  const dLng = toRad(lng2 - lng1);
  const y = Math.sin(dLng) * Math.cos(toRad(lat2));
  const x =
    Math.cos(toRad(lat1)) * Math.sin(toRad(lat2)) -
    Math.sin(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.cos(dLng);

  return (toDeg(Math.atan2(y, x)) + 360) % 360;
}

export const SEVERITY_COLORS = {
  HIGH: "#dc2626",
  MEDIUM: "#d97706",
  LOW: "#3b82f6",
};