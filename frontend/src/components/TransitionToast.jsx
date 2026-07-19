import { useEffect, useState } from "react";
import { STATUS_COLORS } from "../mapUtils";

export default function TransitionToast({ event }) {
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    if (!event) return;
    setVisible(true);
    const timer = setTimeout(() => setVisible(false), 3000);
    return () => clearTimeout(timer);
  }, [event]);

  if (!visible || !event) return null;

  return (
    <div className="transition-toast">
      <span
        className="transition-toast-dot"
        style={{ background: STATUS_COLORS[event.status] || "#6b7280" }}
      />
      <strong>{event.nodeId}</strong> → {event.status}
    </div>
  );
}