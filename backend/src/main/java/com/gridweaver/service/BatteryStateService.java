package com.gridweaver.service;

import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;

import com.gridweaver.statemachine.BatteryEvent;
import com.gridweaver.statemachine.BatteryState;

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

    /**
     * Evaluates grid load for a node and fires the matching transition event.
     * Returns the resulting BatteryState after evaluation.
     */
    public BatteryState evaluate(String nodeId, double gridLoad) {
        StateMachine<BatteryState, BatteryEvent> sm = machineFor(nodeId);
        BatteryState current = sm.getState().getId();

        BatteryEvent event = resolveEvent(current, gridLoad);
        if (event != null) {
            sm.sendEvent(reactor.core.publisher.Mono.just(
                    org.springframework.messaging.support.MessageBuilder
                        .withPayload(event).build()
            )).blockLast();
        }

        BatteryState result = sm.getState().getId();
        log.debug("[STATE] node={} load={} {} -> {}", nodeId, gridLoad, current, result);
        return result;
    }

    private BatteryEvent resolveEvent(BatteryState current, double gridLoad) {
        if (gridLoad > DISCHARGE_THRESHOLD && current != BatteryState.DISCHARGING) {
            return BatteryEvent.START_DISCHARGING;
        }
        if (gridLoad < CHARGE_THRESHOLD && current != BatteryState.CHARGING) {
            return BatteryEvent.START_CHARGING;
        }
        if (gridLoad >= CHARGE_THRESHOLD && gridLoad <= DISCHARGE_THRESHOLD) {
            if (current == BatteryState.CHARGING) return BatteryEvent.STOP_CHARGING;
            if (current == BatteryState.DISCHARGING) return BatteryEvent.STOP_DISCHARGING;
        }
        return null;
    }

    public BatteryState getCurrentState(String nodeId) {
        return machineFor(nodeId).getState().getId();
    }
}