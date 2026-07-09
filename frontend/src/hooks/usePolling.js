import { useEffect, useRef, useState } from "react";

/**
 * Polls an async function on an interval and returns the latest result.
 * Stops polling automatically when the component unmounts.
 *
 * @param {Function} fetchFn - async function returning data
 * @param {number} intervalMs - polling interval
 * @param {boolean} enabled - whether polling is active
 */
export function usePolling(fetchFn, intervalMs = 2000, enabled = true) {
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);
  const savedFetchFn = useRef(fetchFn);

  useEffect(() => {
    savedFetchFn.current = fetchFn;
  }, [fetchFn]);

  useEffect(() => {
    if (!enabled) return;

    let cancelled = false;

    const tick = async () => {
      try {
        const result = await savedFetchFn.current();
        if (!cancelled) {
          setData(result);
          setError(null);
        }
      } catch (err) {
        if (!cancelled) setError(err.message);
      }
    };

    tick(); // fire immediately, then on interval
    const id = setInterval(tick, intervalMs);

    return () => {
      cancelled = true;
      clearInterval(id);
    };
  }, [intervalMs, enabled]);

  return { data, error };
}