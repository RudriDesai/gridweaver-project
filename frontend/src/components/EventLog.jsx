import { useState, useRef, useEffect, memo } from "react";
import { fetchAuditEventsPaged } from "../services/api";
import { usePolling } from "../hooks/usePolling";
import { useWebSocket } from "../hooks/useWebSocket";
import "./EventLog.css";

const STATE_OPTIONS = ["", "CHARGING", "DISCHARGING", "IDLE", "FAULT"];
const MAX_LIVE_EVENTS = 300;


function EventLog() {
  const [nodeId, setNodeId] = useState("");
  const [zoneId, setZoneId] = useState("");
  const [state, setState] = useState("");
  const [page, setPage] = useState(0);
  const size = 15;

  const { lastMessage } = useWebSocket();
  const [liveEvents, setLiveEvents] = useState([]);
  const [unseenCount, setUnseenCount] = useState(0);
  const [autoScroll, setAutoScroll] = useState(true);
  const tableBodyRef = useRef(null);

  const fetchFn = () => fetchAuditEventsPaged({ nodeId, zoneId, state, page, size });
  const { data, error } = usePolling(fetchFn, 3000);

  // Phase B17 — merge live WebSocket events onto the front of page 0 only.
  // Once the user filters or pages away from "latest", live events still
  // accumulate quietly (counted in the badge) without disrupting their view.
  useEffect(() => {
    if (lastMessage?.type !== "AUDIT_EVENT" || !lastMessage.event) return;

    setLiveEvents((prev) => {
      // Remove existing event with the same eventId
      const filtered = prev.filter(
        (event) => event.eventId !== lastMessage.event.eventId
      );

      return [lastMessage.event, ...filtered].slice(0, MAX_LIVE_EVENTS);
    });

    const viewingLatest = page === 0 && !nodeId && !zoneId && !state;

    if (!viewingLatest || !autoScroll) {
      setUnseenCount((c) => c + 1);
    } else if (tableBodyRef.current) {
      tableBodyRef.current.scrollTop = 0;
    }
  }, [lastMessage, page, nodeId, zoneId, state, autoScroll]); // eslint-disable-line react-hooks/exhaustive-deps

  const viewingLatest = page === 0 && !nodeId && !zoneId && !state;

  // Merge: live events first (deduped by eventId), then the REST page fills the rest.
  const baseEvents = data?.events ?? [];

  const liveIds = new Set(liveEvents.map((e) => e.eventId));

  const mergedEvents = viewingLatest
    ? [...liveEvents, ...baseEvents.filter((e) => !liveIds.has(e.eventId))]
    : baseEvents;

  // Final deduplication by eventId
  const events = Array.from(
    new Map(
      mergedEvents.map((event) => [event.eventId, event])
    ).values()
  );

  const totalPages = data?.totalPages ?? 0;

  function applyFilters(e) {
    e.preventDefault();
    setPage(0);
    setUnseenCount(0);
  }

  function jumpToLatest() {
    setPage(0);
    setNodeId("");
    setZoneId("");
    setState("");
    setUnseenCount(0);
    setAutoScroll(true);
    if (tableBodyRef.current) tableBodyRef.current.scrollTop = 0;
  }

  return (
    <div className="event-log-panel">
      <div className="event-log-header">
        <h3>
          Event Log
          {unseenCount > 0 && (
            <button className="event-log-badge" onClick={jumpToLatest}>
              {unseenCount} new
            </button>
          )}
        </h3>
        <label className="event-log-autoscroll">
          <input
            type="checkbox"
            checked={autoScroll}
            onChange={(e) => setAutoScroll(e.target.checked)}
          />
          Auto-scroll
        </label>
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
      {!error && events.length === 0 && <p className="event-log-empty">No matching events.</p>}

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
            <tbody ref={tableBodyRef} className="event-log-tbody-scroll">
              {events.map((e) => (
                <tr key={e.eventId} className={liveIds.has(e.eventId) ? "event-row-live" : ""}>
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

          {!viewingLatest && (
            <div className="event-log-pagination">
              <button disabled={page === 0} onClick={() => setPage((p) => p - 1)}>← Prev</button>
              <span>Page {page + 1} of {Math.max(totalPages, 1)} ({data?.totalElements ?? 0} events)</span>
              <button disabled={page + 1 >= totalPages} onClick={() => setPage((p) => p + 1)}>Next →</button>
            </div>
          )}
        </>
      )}
    </div>
  );
}

export default memo(EventLog);