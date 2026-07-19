export const STATUS_COLORS = {
  CHARGING: "#22c55e",
  DISCHARGING: "#f97316",
  IDLE: "#3b82f6",
  FAULT: "#ef4444",
};

export const STATE_DESCRIPTIONS = {
  CHARGING: "Battery is charging (low grid load)",
  DISCHARGING: "Battery is discharging (high grid load)",
  IDLE: "Battery idle (load within normal range)",
  FAULT: "Battery fault detected",
};

export function timeAgo(timestamp) {
  if (!timestamp) return "unknown";

  const seconds = Math.floor((Date.now() - timestamp) / 1000);

  if (seconds < 5) return "just now";
  if (seconds < 60) return `${seconds}s ago`;

  return `${Math.floor(seconds / 60)}m ago`;
}