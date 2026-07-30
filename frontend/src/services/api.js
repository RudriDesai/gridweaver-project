const BASE_URL = "http://localhost:8080/api";

async function handleResponse(res) {
  if (!res.ok) {
    let message = `Request failed: ${res.status}`;
    try {
      const body = await res.json();
      message = body.error || body.message || message;
    } catch {
      // response wasn't JSON — keep default message
    }
    throw new Error(message);
  }
  return res.json();
}

export async function fetchAllNodes() {
  const res = await fetch(`${BASE_URL}/nodes`);
  return handleResponse(res);
}

export async function initMockNodes(count) {
  const res = await fetch(`${BASE_URL}/nodes/init/${count}`);
  return handleResponse(res);
}

export async function fetchNodeById(nodeId) {
  const res = await fetch(`${BASE_URL}/nodes/${nodeId}`);
  return handleResponse(res);
}

export async function fetchNodeHistory(nodeId, limit = 5) {
  const res = await fetch(`${BASE_URL}/states/history/${nodeId}?limit=${limit}`);
  return handleResponse(res);
}

export async function fetchHealth() {
  const res = await fetch(`${BASE_URL}/health`);
  return handleResponse(res);
}

export async function fetchWsMetrics() {
  const res = await fetch(`${BASE_URL}/ws/metrics`);
  return handleResponse(res);
}

export async function startSimulator(nodeCount, messagesPerNode) {
  const res = await fetch(
    `${BASE_URL}/simulator/start?nodeCount=${nodeCount}&messagesPerNode=${messagesPerNode}`,
    { method: "POST" }
  );
  return handleResponse(res);
}

export async function fetchSimulatorStatus() {
  const res = await fetch(`${BASE_URL}/simulator/status`);
  return handleResponse(res);
}

export async function fetchLastKafkaEvent(nodeId) {
  const res = await fetch(`${BASE_URL}/kafka/consumer/last-event/${nodeId}`);
  if (res.status === 204) return null;
  return handleResponse(res);
}

export async function fetchZoneAnalytics() {
  const res = await fetch(`${BASE_URL}/analytics/zones`);
  return handleResponse(res);
}

export async function fetchConsumerMonitoring() {
  const res = await fetch(`${BASE_URL}/kafka/consumer/monitoring`);
  return handleResponse(res);}

export async function fetchProducerMonitoring() {
  const res = await fetch(`${BASE_URL}/kafka/producer/monitoring`);
  return handleResponse(res);
}

export async function fetchBalancingRecommendations() {
  const res = await fetch(`${BASE_URL}/balancing/recommendations`);
  return handleResponse(res);
}

export async function fetchAuditEvents(limit = 100) {
  const res = await fetch(`${BASE_URL}/audit/events?limit=${limit}`);
  return handleResponse(res);
}

export async function fetchBalancingMetrics() {
  const res = await fetch(`${BASE_URL}/balancing/metrics`);
  return handleResponse(res);
}

export async function fetchAuditEventsPaged(
  { nodeId, zoneId, state, from, to, page = 0, size = 20 } = {}
) {
  const params = new URLSearchParams();

  if (nodeId) params.set("nodeId", nodeId);
  if (zoneId) params.set("zoneId", zoneId);
  if (state) params.set("state", state);
  if (from) params.set("from", from);
  if (to) params.set("to", to);

  params.set("page", page);
  params.set("size", size);

  const res = await fetch(
    `${BASE_URL}/audit/events/query?${params.toString()}`
  );

  return handleResponse(res);
}

export async function fetchAuditStatistics() {
  const res = await fetch(`${BASE_URL}/audit/statistics`);
  return handleResponse(res);
}

export function getAuditExportUrl({
  nodeId,
  zoneId,
  state,
  from,
  to,
} = {}) {
  const params = new URLSearchParams();

  if (nodeId) params.set("nodeId", nodeId);
  if (zoneId) params.set("zoneId", zoneId);
  if (state) params.set("state", state);
  if (from) params.set("from", from);
  if (to) params.set("to", to);

  return `${BASE_URL}/audit/export?${params.toString()}`;
}