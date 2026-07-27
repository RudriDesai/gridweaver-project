import { useEffect, useState, memo } from "react";
import { fetchProducerMonitoring } from "../../services/api";
import "./ProducerMonitoringDashboard.css";

const HISTORY_LENGTH = 20;

function Sparkline({ values, max }) {
  return (
    <div className="sparkline">
      {values.map((v, i) => (
        <span
          key={i}
          className="sparkline-bar"
          style={{
            height: `${max > 0 ? Math.max(4, (v / max) * 100) : 4}%`,
          }}
        />
      ))}
    </div>
  );
}

function ProducerMonitoringDashboard() {
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);

  const [rateHistory, setRateHistory] = useState([]);
  const [latencyHistory, setLatencyHistory] = useState([]);

  useEffect(() => {
    let cancelled = false;

    const tick = async () => {
      if (document.hidden) return;

      try {
        const result = await fetchProducerMonitoring();

        if (cancelled) return;

        setData(result);
        setError(null);

        setRateHistory((prev) => [
          ...prev.slice(-(HISTORY_LENGTH - 1)),
          result.publishRatePerSec,
        ]);

        setLatencyHistory((prev) => [
          ...prev.slice(-(HISTORY_LENGTH - 1)),
          result.avgLatencyMs,
        ]);
      } catch (err) {
        if (!cancelled) {
          setError(err.message);
        }
      }
    };

    tick();

    const intervalId = setInterval(tick, 1000);

    document.addEventListener("visibilitychange", tick);

    return () => {
      cancelled = true;
      clearInterval(intervalId);
      document.removeEventListener("visibilitychange", tick);
    };
  }, []);

  return (
    <div className="monitoring-dashboard">
      <h3>Producer Monitoring</h3>

      {error && (
        <p className="status-banner error">
          Monitoring unavailable
        </p>
      )}

      {data && (
        <div className="monitoring-grid">

          <div className="monitoring-card">
            <span className="monitoring-value">
              {data.publishRatePerSec.toFixed(1)}
            </span>
            <span className="monitoring-label">
              Publish rate/sec
            </span>

            <Sparkline
              values={rateHistory}
              max={Math.max(1, ...rateHistory)}
            />
          </div>

          <div className="monitoring-card">
            <span className="monitoring-value">
              {data.queueSize}
            </span>
            <span className="monitoring-label">
              In-flight queue
            </span>
          </div>

          <div
            className={`monitoring-card ${
              data.failedPublishes > 0 ? "warn" : ""
            }`}
          >
            <span className="monitoring-value">
              {data.failedPublishes}
            </span>
            <span className="monitoring-label">
              Failed publishes
            </span>
          </div>

          <div className="monitoring-card">
            <span className="monitoring-value">
              {data.avgLatencyMs.toFixed(1)} ms
            </span>
            <span className="monitoring-label">
              Avg latency
            </span>

            <Sparkline
              values={latencyHistory}
              max={Math.max(1, ...latencyHistory)}
            />
          </div>

        </div>
      )}
    </div>
  );
}

export default memo(ProducerMonitoringDashboard);