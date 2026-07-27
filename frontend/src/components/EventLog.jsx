import { useMemo, useState, memo } from "react";
import { fetchAuditEvents } from "../services/api";
import { usePolling } from "../hooks/usePolling";
import "./EventLog.css";

/**
 * Phase B15 — Event Log page with a searchable audit table.
 * Search filters client-side across nodeId, previousState, newState, reason.
 */
function EventLog() {
  const { data: events, error } = usePolling(() => fetchAuditEvents(200), 3000);
  const [search, setSearch] = useState("");

  const filtered = useMemo(() => {
    if (!events) return [];
    if (!search.trim()) return events;
    const q = search.toLowerCase();
    return events.filter((e) =>
      [e.nodeId, e.previousState, e.newState, e.reason]
        .join(" ")
        .toLowerCase()
        .includes(q)
    );
  }, [events, search]);

  return (
    <div className="event-log-panel">
      <div className="event-log-header">
        <h3>Event Log</h3>
        <input
          className="event-log-search"
          type="text"
          placeholder="Search by node, state, or reason..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>

      {error && <p className="status-banner error">Audit log unavailable</p>}

      {!error && filtered.length === 0 && (
        <p className="event-log-empty">No matching events.</p>
      )}

      {filtered.length > 0 && (
        <table className="event-log-table">
          <thead>
            <tr>
              <th>Time</th>
              <th>Node</th>
              <th>From</th>
              <th>To</th>
              <th>Reason</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map((e) => (
              <tr key={e.eventId}>
                <td>{new Date(e.timestamp).toLocaleTimeString()}</td>
                <td>{e.nodeId}</td>
                <td>{e.previousState}</td>
                <td>{e.newState}</td>
                <td>{e.reason}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

export default memo(EventLog);