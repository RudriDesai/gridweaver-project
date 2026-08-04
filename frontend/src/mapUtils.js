export const STATUS_COLORS = {
  CHARGING: "#34d399",
  DISCHARGING: "#ffb020",
  IDLE: "#38bdf8",
  FAULT: "#f43f5e",
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