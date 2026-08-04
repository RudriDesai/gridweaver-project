const LEGEND_ITEMS = [
  { label: "Charging", color: "#34d399" },
  { label: "Discharging", color: "#ffb020" },
  { label: "Idle", color: "#38bdf8" },
  { label: "Fault", color: "#f43f5e" },
];

export default function MapLegend() {
  return (
    <div className="map-legend">
      {LEGEND_ITEMS.map((item) => (
        <div key={item.label} className="legend-row">
          <span className="legend-dot" style={{ background: item.color }} />
          {item.label}
        </div>
      ))}
    </div>
  );
}