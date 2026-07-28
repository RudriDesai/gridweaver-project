import { useState, memo } from "react";
import { fetchAuditEventsPaged } from "../services/api";
import { usePolling } from "../hooks/usePolling";
import "./EventLog.css";

const STATE_OPTIONS = ["", "CHARGING", "DISCHARGING", "IDLE", "FAULT"];

/**
 * Phase B16 — Event Log connected to the server-side filtered/paginated
 * audit API. Client-side search from Day 1 is replaced by real filters.
 */
function EventLog() {
  const [nodeId, setNodeId] = useState("");
  const [zoneId, setZoneId] = useState("");
  const [state, setState] = useState("");
  const [page, setPage] = useState(0);
  const size = 15;

  const fetchFn = () => fetchAuditEventsPaged({ nodeId, zoneId, state, page, size });
  const { data, error } = usePolling(fetchFn, 3000);

  const events = data?.events ?? [];
  const totalPages = data?.totalPages ?? 0;

  function applyFilters(e) {
    e.preventDefault();
    setPage(0); // reset to first page whenever filters change
  }

  return (
    <div className="event-log-panel">
      <div className="event-log-header">
        <h3>Event Log</h3>
      </div>

      <form className="event-log-filters" onSubmit={applyFilters}>
        <input
          type="text"
          placeholder="Node ID (e.g. NODE-0001)"
          value={nodeId}
          onChange={(e) => setNodeId(e.target.value)}
        />
        <input
          type="text"
          placeholder="Zone (e.g. ZONE-A)"
          value={zoneId}
          onChange={(e) => setZoneId(e.target.value)}
        />
        <select value={state} onChange={(e) => setState(e.target.value)}>
          {STATE_OPTIONS.map((s) => (
            <option key={s} value={s}>{s || "All states"}</option>
          ))}
        </select>
        <button type="submit">Apply</button>
      </form>

      {error && <p className="status-banner error">Audit log unavailable</p>}

      {!error && events.length === 0 && (
        <p className="event-log-empty">No matching events.</p>
      )}

      {events.length > 0 && (
        <>
          <table className="event-log-table">
            <thead>
              <tr>
                <th>Time</th>
                <th>Node</th>
                <th>Zone</th>
                <th>From</th>
                <th>To</th>
                <th>Reason</th>
              </tr>
            </thead>
            <tbody>
              {events.map((e) => (
                <tr key={e.eventId}>
                  <td>{new Date(e.timestamp).toLocaleTimeString()}</td>
                  <td>{e.nodeId}</td>
                  <td>{e.zoneId || "—"}</td>
                  <td>{e.previousState}</td>
                  <td>{e.newState}</td>
                  <td>{e.reason}</td>
                </tr>
              ))}
            </tbody>
          </table>

          <div className="event-log-pagination">
            <button disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
              ← Prev
            </button>
            <span>
              Page {page + 1} of {Math.max(totalPages, 1)} ({data.totalElements} events)
            </span>
            <button
              disabled={page + 1 >= totalPages}
              onClick={() => setPage((p) => p + 1)}
            >
              Next →
            </button>
          </div>
        </>
      )}
    </div>
  );
}

export default memo(EventLog);