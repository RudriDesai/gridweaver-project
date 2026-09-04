package com.gridweaver.util;

import java.lang.management.ManagementFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sun.management.UnixOperatingSystemMXBean;

/**
 * Reads OS-level file-descriptor usage for the running JVM.
 *
 * Why this exists: the WebSocket simulator and the WebSocket server run
 * in the SAME JVM process on localhost. Every simulated node consumes
 * at least 2 file descriptors (the client socket + the server-accepted
 * socket for that same connection), plus Kafka producer/consumer
 * connections and normal log/file handles. If the process ulimit
 * (soft nofile) is left at the OS default (often 1024, sometimes
 * 4096-10000), then a batch that lands on top of an already-large
 * pool of open/lingering connections will start failing with
 * "Too many open files" even though the code itself is correct.
 *
 * This is almost certainly why small fresh batches (2-3k) succeed but
 * layering more nodes on top of already-connected ones causes a wave
 * of failures: you are approaching or hitting the fd ceiling, not
 * hitting a logic bug.
 */
@Component
public class SystemResourceMonitor {

    private static final Logger log = LoggerFactory.getLogger(SystemResourceMonitor.class);

    /** Roughly how many fds one simulated node consumes end-to-end (client + server socket). */
    public static final int ESTIMATED_FDS_PER_NODE = 2;

    /** Keep this much headroom free so Kafka connections / logging / JVM internals don't starve. */
    private static final double SAFE_UTILIZATION_THRESHOLD = 0.85;

    public FdStats currentFdStats() {
        var osBean = ManagementFactory.getOperatingSystemMXBean();
        if (osBean instanceof UnixOperatingSystemMXBean unixBean) {
            long open = unixBean.getOpenFileDescriptorCount();
            long max = unixBean.getMaxFileDescriptorCount();
            return new FdStats(open, max);
        }
        // Non-Unix JVM (e.g. some Windows setups) - can't introspect; report unknown.
        return new FdStats(-1, -1);
    }

    /**
     * Logs current fd usage and warns loudly if a batch of the given size
     * is likely to push the process over a safe utilization threshold.
     * Does not block the caller - this is observability, not a hard gate,
     * because the safe threshold is only an estimate.
     */
    public void checkHeadroomForBatch(int additionalNodeCount) {
        FdStats stats = currentFdStats();
        if (stats.max() < 0) {
            log.info("[FD-CHECK] Unable to read fd usage on this platform (non-Unix JVM). " +
                    "If connections start failing at scale, check OS-level limits manually.");
            return;
        }

        long projected = stats.open() + (long) additionalNodeCount * ESTIMATED_FDS_PER_NODE;
        double projectedUtilization = (double) projected / stats.max();

        log.info("[FD-CHECK] open={} max={} projectedAfterBatch={} ({}% of limit)",
                stats.open(), stats.max(), projected, Math.round(projectedUtilization * 100));

        if (projectedUtilization >= SAFE_UTILIZATION_THRESHOLD) {
            log.warn("[FD-CHECK] WARNING: starting {} more nodes will push open file descriptors " +
                    "to ~{}% of the process limit ({}). Expect connection failures. " +
                    "Raise the OS ulimit (ulimit -n) / systemd LimitNOFILE for this process " +
                    "and re-check before running larger batches.",
                    additionalNodeCount, Math.round(projectedUtilization * 100), stats.max());
        }
    }

    public record FdStats(long open, long max) {
    }
}
