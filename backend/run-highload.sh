#!/usr/bin/env bash
# Run the GridWeaver backend with OS limits raised for high WebSocket
# connection counts (8k-45k simulated nodes).
#
# WHY THIS SCRIPT EXISTS
# -----------------------
# The simulator (client) and the WebSocket server run inside the SAME
# JVM process, both talking over localhost. Every simulated node needs:
#   - 1 file descriptor for the outbound client socket
#   - 1 file descriptor for the server-accepted socket
#   - 1 ephemeral local port (client side)
# On most dev machines the default per-process open-file limit
# (ulimit -n) is 1024-4096. That ceiling is shared by ALL sockets in
# the process - client + server + Kafka connections + log files.
#
# This is why a fresh 2-3k batch (roughly 4-6k fds) mostly succeeds,
# but adding more load on top of nodes that are already connected
# (fds already consumed, some sockets sitting in TIME_WAIT) pushes the
# process over the limit and a wave of connections start failing with
# "Too many open files" - which on the client surfaces simply as a
# failed handshake, not an obvious error message.
#
# Run this script instead of `mvn spring-boot:run` / the jar directly
# when you intend to push 8k+ simulated connections.

set -e

# Raise the soft limit for this shell + everything it launches.
# (Hard limit must already allow this - see the one-time OS setup below
# if you get "Operation not permitted".)
ulimit -n 65535

echo "== ulimit -n now: $(ulimit -n) =="
echo "== ephemeral port range: $(cat /proc/sys/net/ipv4/ip_local_port_range 2>/dev/null || echo 'n/a (not Linux)') =="

# Optional but recommended one-time OS-level tuning (Linux, needs sudo,
# only needs to be run once per machine/reboot - NOT every run):
#   sudo sysctl -w net.ipv4.ip_local_port_range="1024 65535"
#   sudo sysctl -w net.ipv4.tcp_tw_reuse=1
#   sudo sysctl -w net.core.somaxconn=8192
# And to make the ulimit stick without needing this script every time,
# add to /etc/security/limits.conf:
#   your-user   soft   nofile   65535
#   your-user   hard   nofile   65535

cd "$(dirname "$0")"
./mvnw spring-boot:run
