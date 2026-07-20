import { useEffect, useState, useRef } from 'react';
import { API_BASE } from '../../config/api';
import './KafkaProducerStatus.css';

export default function KafkaProducerStatus() {
  const [status, setStatus] = useState('CONNECTING');
  const [publishedCount, setPublishedCount] = useState(0);
  const [failedCount, setFailedCount] = useState(0);
  const [error, setError] = useState(null);
  const pollRef = useRef(null);

  const fetchStatus = async () => {
    try {
      const res = await fetch(`${API_BASE}/api/kafka/producer/status`);
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const data = await res.json();
      setStatus(data.status);
      setPublishedCount(data.publishedCount);
      setFailedCount(data.failedCount);
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
        <span className="kafka-status-title">Kafka Producer</span>
        <span className={`kafka-badge kafka-badge--${status.toLowerCase()}`}>
          {status}
        </span>
      </div>

      <div className="kafka-status-metrics">
        <div className="kafka-metric">
          <span className="kafka-metric-value">{publishedCount}</span>
          <span className="kafka-metric-label">Published</span>
        </div>
        <div className="kafka-metric kafka-metric--error">
          <span className="kafka-metric-value">{failedCount}</span>
          <span className="kafka-metric-label">Failed</span>
        </div>
      </div>

      {error && <div className="kafka-status-error">{error}</div>}
    </div>
  );
}