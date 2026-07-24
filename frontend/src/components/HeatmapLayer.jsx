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
  const debounceRef = useRef(null);

  useEffect(() => {
    // Remove layer if heatmap is disabled
    if (!visible) {
      if (layerRef.current) {
        map.removeLayer(layerRef.current);
        layerRef.current = null;
      }
      return;
    }

    // Phase B14: debounce rapid WebSocket updates
    if (debounceRef.current) {
      clearTimeout(debounceRef.current);
    }

    debounceRef.current = setTimeout(() => {
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

      // Create only once
      if (!layerRef.current) {
        layerRef.current = L.heatLayer(points, {
          radius: 45,
          blur: 35,
          maxZoom: 17,
          max: 1,
        }).addTo(map);
      } else {
        // Phase B14: update existing layer instead of recreating it
        layerRef.current.setLatLngs(points);
      }
    }, 400);

    return () => {
      if (debounceRef.current) {
        clearTimeout(debounceRef.current);
      }
    };
  }, [nodes, mode, visible, map]);

  useEffect(() => {
    return () => {
      if (layerRef.current) {
        map.removeLayer(layerRef.current);
      }
    };
  }, [map]);

  return null;
}