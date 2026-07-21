import { useEffect, useState, useRef } from 'react';
import { API_BASE } from '../../config/api';
import './KafkaConsumerStatus.css';

export default function KafkaConsumerStatus() {
  const [status, setStatus] = useState('CONNECTING');
  const [consumedCount, setConsumedCount] = useState(0);
  const [lastEvent, setLastEvent] = useState(null);
  const [error, setError] = useState(null);
  const pollRef = useRef(null);
  const [eventsPerSecond, setEventsPerSecond] = useState(0);

  const fetchStatus = async () => {
    try {
      const res = await fetch(`${API_BASE}/api/kafka/consumer/status`);
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const data = await res.json();
      setStatus(data.status);
      setConsumedCount(data.consumedCount);
      setEventsPerSecond(data.eventsPerSecond ?? 0);
      setLastEvent(data.lastEvent === 'none' ? null : data.lastEvent);
      setError(null);
    } catch (err) {
      setStatus('DOWN');
      setError(err.message);
    }
  };

  useEffect(() => {
    fetchStatus();
    pollRef.current = setInterval(fetchStatus, 2000);
    return () => clearInterval(pollRef.current);
  }, []);

  return (
    <div className="kafka-status-card">
      <div className="kafka-status-header">
        <span className="kafka-status-title">Kafka Consumer</span>
        <span className={`kafka-badge kafka-badge--${status.toLowerCase()}`}>
          {status}
        </span>
      </div>

      <div className="kafka-status-metrics">
        <div className="kafka-metric">
          <span className="kafka-metric-value">{consumedCount}</span>
          <span className="kafka-metric-label">Consumed</span>
        </div>

        <div className="kafka-metric">
          <span className="kafka-metric-value">
            {eventsPerSecond.toFixed(1)}
          </span>
          <span className="kafka-metric-label">Events/sec</span>
        </div>
      </div>

      {lastEvent && (
        <div className="kafka-last-event">
          <div className="kafka-last-event-title">Last Event</div>
          <div className="kafka-last-event-row">
            <span>Node</span><span>{lastEvent.nodeId}</span>
          </div>
          <div className="kafka-last-event-row">
            <span>Zone</span><span>{lastEvent.zoneId}</span>
          </div>
          <div className="kafka-last-event-row">
            <span>Battery</span><span>{lastEvent.batteryState}</span>
          </div>
        </div>
      )}

      {error && <div className="kafka-status-error">{error}</div>}
    </div>
  );
}