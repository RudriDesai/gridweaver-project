import { useEffect, useRef, useState, useCallback } from "react";

const WS_URL = "ws://localhost:8080/ws/iot";
const RECONNECT_DELAY_MS = 2000;

export function useWebSocket() {
  const [connected, setConnected] = useState(false);
  const [lastMessage, setLastMessage] = useState(null);
  const wsRef = useRef(null);
  const reconnectTimer = useRef(null);
  const manuallyClosedRef = useRef(false);

  const connect = useCallback(() => {
    const ws = new WebSocket(WS_URL);
    wsRef.current = ws;

    ws.onopen = () => {
      setConnected(true);
      console.log("[WS] connected");
    };

    ws.onmessage = (event) => {
      try {
        setLastMessage(JSON.parse(event.data));
      } catch {
        setLastMessage(event.data);
      }
    };

    ws.onclose = () => {
      setConnected(false);
      console.log("[WS] disconnected — retrying in", RECONNECT_DELAY_MS, "ms");
      if (!manuallyClosedRef.current) {
        reconnectTimer.current = setTimeout(connect, RECONNECT_DELAY_MS);
      }
    };

    ws.onerror = (err) => {
      console.warn("[WS] error", err);
      ws.close();
    };
  }, []);

  useEffect(() => {
    manuallyClosedRef.current = false;
    connect();

    return () => {
      manuallyClosedRef.current = true;
      clearTimeout(reconnectTimer.current);
      wsRef.current?.close();
    };
  }, [connect]);

  const send = useCallback((data) => {
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      wsRef.current.send(typeof data === "string" ? data : JSON.stringify(data));
    }
  }, []);

  return { connected, lastMessage, send };
}