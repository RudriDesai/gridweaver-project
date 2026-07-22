import { useEffect, useRef } from "react";
import { useMap } from "react-leaflet";
import * as L from "leaflet";
import "leaflet.heat";

export default function HeatmapLayer({
  nodes,
  mode = "generation",
  visible = true,
}) {
  const map = useMap();
  const layerRef = useRef(null);

  useEffect(() => {
    // Remove layer if heatmap is disabled
    if (!visible) {
      if (layerRef.current) {
        map.removeLayer(layerRef.current);
        layerRef.current = null;
      }
      return;
    }

    // Build heatmap points
    const points = nodes
      .filter(
        (n) =>
          n.latitude != null &&
          n.longitude != null
      )
      .map((n) => {
        const intensity =
          mode === "consumption"
            ? (n.consumption ?? 0)
            : (n.generation ?? 0);

        return [
          n.latitude,
          n.longitude,
          intensity / 100,
        ];
      });

    console.log("Heatmap points:", points.length);

    // Remove previous layer
    if (layerRef.current) {
      map.removeLayer(layerRef.current);
      layerRef.current = null;
    }

    // Create new layer
    layerRef.current = L.heatLayer(points, {
      radius: 45,
      blur: 35,
      maxZoom: 17,
      max: 1,
    }).addTo(map);

    console.log("Layer added:", map.hasLayer(layerRef.current));

    return () => {
      if (layerRef.current) {
        map.removeLayer(layerRef.current);
        layerRef.current = null;
      }
    };
  }, [nodes, mode, visible, map]);

  return null;
}
