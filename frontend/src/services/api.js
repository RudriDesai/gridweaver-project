const BASE_URL = "http://localhost:8080/api";

export async function fetchAllNodes() {
  const res = await fetch(`${BASE_URL}/nodes`);
  if (!res.ok) throw new Error(`Failed to fetch nodes: ${res.status}`);
  return res.json();
}

export async function initMockNodes(count) {
  const res = await fetch(`${BASE_URL}/nodes/init/${count}`);
  if (!res.ok) throw new Error(`Failed to init nodes: ${res.status}`);
  return res.json();
}

export async function fetchNodeById(nodeId) {
  const res = await fetch(`${BASE_URL}/nodes/${nodeId}`);
  if (!res.ok) throw new Error(`Node not found: ${nodeId}`);
  return res.json();
}

export async function fetchHealth() {
  const res = await fetch(`${BASE_URL}/health`);
  return res.json();
}