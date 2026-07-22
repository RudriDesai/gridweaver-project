import { useEffect, useRef } from "react";
import { useMap } from "react-leaflet";
import L from "leaflet";
import "leaflet.heat";

/**
 * Imperative Leaflet heat layer, driven by live node data.
 * mode: "generation" | "consumption" — picks which field weights the heat.
 */
export default function HeatmapLayer({ nodes, mode = "generation", visible = true }) {
  const map = useMap();
  const layerRef = useRef(null);

  useEffect(() => {
    if (!visible) {
      if (layerRef.current) {
        map.removeLayer(layerRef.current);
        layerRef.current = null;
      }
      return;
    }

    const points = nodes
      .filter((n) => n.latitude && n.longitude)
      .map((n) => {
        const intensity = mode === "consumption" ? n.consumption : n.generation;
        return [n.latitude, n.longitude, Math.max(0, Math.min(1, (intensity || 0) / 100))];
      });

    if (!layerRef.current) {
      layerRef.current = L.heatLayer(points, { radius: 28, blur: 20, maxZoom: 17 }).addTo(map);
    } else {
      layerRef.current.setLatLngs(points);
    }
  }, [nodes, mode, visible, map]);

  useEffect(() => {
    return () => {
      if (layerRef.current) map.removeLayer(layerRef.current);
    };
  }, [map]);

  return null;
}