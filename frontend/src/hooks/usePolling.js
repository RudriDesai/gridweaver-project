import { useEffect, useRef, useState } from "react";

/**
 * Polls an async function on an interval and returns the latest result.
 * Stops polling automatically when the component unmounts.
 *
 * Phase B14:
 * - Pauses polling when the browser tab is hidden.
 * - Skips unnecessary re-renders if the response hasn't changed.
 *
 * @param {Function} fetchFn - async function returning data
 * @param {number} intervalMs - polling interval
 * @param {boolean} enabled - whether polling is active
 */
export function usePolling(fetchFn, intervalMs = 2000, enabled = true) {
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);

  const savedFetchFn = useRef(fetchFn);
  const lastSerialized = useRef(null);

  useEffect(() => {
    savedFetchFn.current = fetchFn;
  }, [fetchFn]);

  useEffect(() => {
    if (!enabled) return;

    let cancelled = false;

    const tick = async () => {
      // Phase B14: Skip polling while the tab is hidden
      if (document.hidden) return;

      try {
        const result = await savedFetchFn.current();

        if (cancelled) return;

        // Phase B14: Skip state update if nothing changed
        const serialized = JSON.stringify(result);

        if (serialized !== lastSerialized.current) {
          lastSerialized.current = serialized;
          setData(result);
        }

        setError(null);

      } catch (err) {
        if (!cancelled) {
          setError(err.message);
        }
      }
    };

    // Initial fetch
    tick();

    // Continue polling
    const id = setInterval(tick, intervalMs);

    // Refresh immediately when the tab becomes visible
    document.addEventListener("visibilitychange", tick);

    return () => {
      cancelled = true;
      clearInterval(id);
      document.removeEventListener("visibilitychange", tick);
    };
  }, [intervalMs, enabled]);

  return { data, error };
}