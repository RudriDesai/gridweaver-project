const LEGEND_ITEMS = [
  { label: "Charging", color: "#22c55e" },
  { label: "Discharging", color: "#f97316" },
  { label: "Idle", color: "#3b82f6" },
  { label: "Fault", color: "#ef4444" },
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