package com.gridweaver.service;

import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineEventResult;
import org.springframework.statemachine.config.StateMachineFactory;

import com.gridweaver.model.StateTransitionRecord;
import com.gridweaver.statemachine.BatteryEvent;
import com.gridweaver.statemachine.BatteryState;

import jakarta.annotation.PostConstruct;

/**
 * Evaluates grid load against thresholds and drives each node's
 * Spring State Machine instance accordingly.
 *
 * Day 5 hardening:
 *  - Per-node ReentrantLock: StateMachine.sendEvent() is not safe for
 *    concurrent calls against the same instance. Virtual threads can
 *    deliver telemetry for the same nodeId concurrently, so each node
 *    gets its own lock.
 *  - Acceptance is checked against the real
 *    StateMachineEventResult.ResultType.ACCEPTED instead of a
 *    hand-maintained isValidTransition() map that can silently drift
 *    out of sync with BatteryStateMachineConfig.
 *  - totalEvaluations / rejectedTransitions / activeMachineCount are
 *    tracked so state machine health can be audited once Kafka adds
 *    volume in Week 3.
 *
 * Thresholds:
 *   gridLoad > 80%         -> DISCHARGING
 *   gridLoad < 20%         -> CHARGING
 *   20% <= gridLoad <= 80% -> IDLE
 *   NaN / < 0 / > 100      -> FAULT
 */
@Service
public class BatteryStateService {

    private static final Logger log = LoggerFactory.getLogger(BatteryStateService.class);

    private static final double DISCHARGE_THRESHOLD = 80.0;
    private static final double CHARGE_THRESHOLD = 20.0;
    private static final int MAX_HISTORY = 500;

    public interface StateChangeListener {
        void onStateChanged(String nodeId, BatteryState oldState, BatteryState newState);
    }

    private final StateMachineFactory<BatteryState, BatteryEvent> stateMachineFactory;
    private final List<StateChangeListener> listeners = new CopyOnWriteArrayList<>();
    private final Deque<StateTransitionRecord> history = new ConcurrentLinkedDeque<>();

    // One state machine instance per node, kept in memory
    private final ConcurrentHashMap<String, StateMachine<BatteryState, BatteryEvent>> machines =
            new ConcurrentHashMap<>();

    // Serializes access per node — sendEvent() is not thread-safe for
    // concurrent calls against the same StateMachine instance.
    private final ConcurrentHashMap<String, ReentrantLock> nodeLocks =
            new ConcurrentHashMap<>();

    private final AtomicInteger totalEvaluations = new AtomicInteger(0);
    private final AtomicInteger rejectedTransitions = new AtomicInteger(0);

    public BatteryStateService(StateMachineFactory<BatteryState, BatteryEvent> stateMachineFactory) {
        this.stateMachineFactory = stateMachineFactory;
    }

    public void addListener(StateChangeListener listener) {
        listeners.add(listener);
    }

    private StateMachine<BatteryState, BatteryEvent> machineFor(String nodeId) {
        return machines.computeIfAbsent(nodeId, id -> {
            StateMachine<BatteryState, BatteryEvent> sm = stateMachineFactory.getStateMachine(id);
            sm.startReactively().block();
            return sm;
        });
    }

    /**
     * Evaluates grid load for a node and fires the matching transition event.
     * Returns the resulting BatteryState after evaluation.
     */
    public BatteryState evaluate(String nodeId, double gridLoad) {
        ReentrantLock lock = nodeLocks.computeIfAbsent(nodeId, id -> new ReentrantLock());
        lock.lock();
        try {
            StateMachine<BatteryState, BatteryEvent> sm = machineFor(nodeId);
            BatteryState current = sm.getState().getId();

            totalEvaluations.incrementAndGet();
            BatteryEvent event = resolveEvent(current, gridLoad);

            if (event != null) {
                boolean accepted = sendEventAndCheckAccepted(sm, event);
                if (!accepted) {
                    rejectedTransitions.incrementAndGet();
                    log.warn("[STATE-REJECTED] node={} event={} from={} (no matching transition)",
                            nodeId, event, current);
                }
            }

            BatteryState result = sm.getState().getId();
            if (result != current) {
                listeners.forEach(l -> l.onStateChanged(nodeId, current, result));
            }

            log.debug("[STATE] node={} load={} {} -> {}", nodeId, gridLoad, current, result);
            return result;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Sends the event and checks Spring's own acceptance result rather than
     * a hand-maintained map — this is the source of truth, so it can never
     * drift out of sync with BatteryStateMachineConfig.
     */
    private boolean sendEventAndCheckAccepted(StateMachine<BatteryState, BatteryEvent> sm, BatteryEvent event) {
        List<StateMachineEventResult<BatteryState, BatteryEvent>> results = sm.sendEvent(
                reactor.core.publisher.Mono.just(
                        org.springframework.messaging.support.MessageBuilder
                                .withPayload(event).build()
                )
        ).collectList().block();

        return results != null && results.stream()
                .anyMatch(r -> r.getResultType() == StateMachineEventResult.ResultType.ACCEPTED);
    }

    private BatteryEvent resolveEvent(BatteryState current, double gridLoad) {

        // Invalid sensor reading
        if (Double.isNaN(gridLoad) || gridLoad < 0 || gridLoad > 100) {
            return current == BatteryState.FAULT
                    ? null
                    : BatteryEvent.FAULT_DETECTED;
        }

        // Recover from fault
        if (current == BatteryState.FAULT) {
            return BatteryEvent.FAULT_CLEARED;
        }

        if (gridLoad > DISCHARGE_THRESHOLD && current != BatteryState.DISCHARGING) {
            return BatteryEvent.START_DISCHARGING;
        }

        if (gridLoad < CHARGE_THRESHOLD && current != BatteryState.CHARGING) {
            return BatteryEvent.START_CHARGING;
        }

        if (gridLoad >= CHARGE_THRESHOLD && gridLoad <= DISCHARGE_THRESHOLD) {
            if (current == BatteryState.CHARGING)
                return BatteryEvent.STOP_CHARGING;

            if (current == BatteryState.DISCHARGING)
                return BatteryEvent.STOP_DISCHARGING;
        }

        return null;
    }

    public BatteryState getCurrentState(String nodeId) {
        return machineFor(nodeId).getState().getId();
    }

    @PostConstruct
    public void recordOwnTransitions() {
        addListener((nodeId, oldState, newState) -> {
            history.addFirst(new StateTransitionRecord(nodeId, oldState.name(), newState.name()));
            while (history.size() > MAX_HISTORY) {
                history.removeLast();
            }
        });
    }

    public List<StateTransitionRecord> getRecentHistory(int limit) {
        return history.stream().limit(limit).collect(Collectors.toList());
    }

    public List<StateTransitionRecord> getHistoryForNode(String nodeId, int limit) {
        return history.stream()
                .filter(r -> r.getNodeId().equals(nodeId))
                .limit(limit)
                .collect(Collectors.toList());
    }

    // ── Metrics (Day 5) ────────────────────────────────

    public int getTotalEvaluations() {
        return totalEvaluations.get();
    }

    public int getRejectedTransitions() {
        return rejectedTransitions.get();
    }

    public int getActiveMachineCount() {
        return machines.size();
    }
}