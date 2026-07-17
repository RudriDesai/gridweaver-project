package com.gridweaver.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;

import com.gridweaver.statemachine.BatteryEvent;
import com.gridweaver.statemachine.BatteryState;

import jakarta.annotation.PostConstruct;

import com.gridweaver.model.StateTransitionRecord;

import java.util.Deque;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Evaluates grid load against thresholds and drives each node's
 * Spring State Machine instance accordingly.
 *
 * Thresholds:
 *   gridLoad > 80%      -> DISCHARGING
 *   gridLoad < 20%       -> CHARGING
 *   20% <= gridLoad <= 80% -> IDLE
 *   simulated fault flag -> FAULT
 */
@Service
public class BatteryStateService {

    private static final Logger log = LoggerFactory.getLogger(BatteryStateService.class);

    private static final double DISCHARGE_THRESHOLD = 80.0;
    private static final double CHARGE_THRESHOLD = 20.0;
    private final Deque<StateTransitionRecord> history = new ConcurrentLinkedDeque<>();
    private static final int MAX_HISTORY = 500; // cap memory usage

    private final StateMachineFactory<BatteryState, BatteryEvent> stateMachineFactory;

    // One state machine instance per node, kept in memory
    private final ConcurrentHashMap<String, StateMachine<BatteryState, BatteryEvent>> machines =
            new ConcurrentHashMap<>();

    public BatteryStateService(StateMachineFactory<BatteryState, BatteryEvent> stateMachineFactory) {
        this.stateMachineFactory = stateMachineFactory;
    }

    private StateMachine<BatteryState, BatteryEvent> machineFor(String nodeId) {
        return machines.computeIfAbsent(nodeId, id -> {
            StateMachine<BatteryState, BatteryEvent> sm = stateMachineFactory.getStateMachine(id);
            sm.startReactively().block();
            return sm;
        });
    }

    private final List<StateChangeListener> listeners = new CopyOnWriteArrayList<>();

    public interface StateChangeListener {
        void onStateChanged(String nodeId,
                            BatteryState oldState,
                            BatteryState newState);
    }

    public void addListener(StateChangeListener listener) {
        listeners.add(listener);
    }

    /**
     * Evaluates grid load for a node and fires the matching transition event.
     * Returns the resulting BatteryState after evaluation.
     */
 // updated evaluate() — validates before firing
    public BatteryState evaluate(String nodeId, double gridLoad) {
        StateMachine<BatteryState, BatteryEvent> sm = machineFor(nodeId);
        BatteryState current = sm.getState().getId();

        BatteryEvent event = resolveEvent(current, gridLoad);
        if (event != null && isValidTransition(current, event)) {
            sm.sendEvent(reactor.core.publisher.Mono.just(
                    org.springframework.messaging.support.MessageBuilder.withPayload(event).build()
            )).blockLast();
        } else if (event != null) {
            log.warn("[STATE-REJECTED] node={} tried {} from {} — invalid transition", nodeId, event, current);
        }

        BatteryState result = sm.getState().getId();
        if (result != current) {
            listeners.forEach(l -> l.onStateChanged(nodeId, current, result));
        }
        return result;
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
    /**
     * Guards against firing events the current state doesn't support.
     * Returns false (and skips the event) if invalid, instead of letting
     * Spring State Machine silently no-op.
     */
    private boolean isValidTransition(BatteryState from, BatteryEvent event) {
        return switch (from) {
            case IDLE ->
                event == BatteryEvent.START_CHARGING
                || event == BatteryEvent.START_DISCHARGING
                || event == BatteryEvent.FAULT_DETECTED;

            case CHARGING ->
                event == BatteryEvent.STOP_CHARGING
                || event == BatteryEvent.FAULT_DETECTED;

            case DISCHARGING ->
                event == BatteryEvent.STOP_DISCHARGING
                || event == BatteryEvent.FAULT_DETECTED;

            case FAULT ->
                event == BatteryEvent.FAULT_CLEARED;
        };
    }}