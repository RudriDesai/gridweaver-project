package com.gridweaver.controller;

import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gridweaver.statemachine.BatteryEvent;
import com.gridweaver.statemachine.BatteryState;

@RestController
@RequestMapping("/test")
public class StateMachineTestController {

    private final StateMachineFactory<BatteryState, BatteryEvent> factory;

    public StateMachineTestController(
            StateMachineFactory<BatteryState, BatteryEvent> factory) {
        this.factory = factory;
    }

    @GetMapping
    public String test() {

        StateMachine<BatteryState, BatteryEvent> machine = factory.getStateMachine();

        machine.start();

        return machine.getState().getId().name();
    }
}