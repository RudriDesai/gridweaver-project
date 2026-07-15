package com.gridweaver.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import com.gridweaver.statemachine.BatteryEvent;
import com.gridweaver.statemachine.BatteryState;

@Configuration
@EnableStateMachineFactory
public class BatteryStateMachineConfig
        extends StateMachineConfigurerAdapter<BatteryState, BatteryEvent> {

    @Override
    public void configure(StateMachineStateConfigurer<BatteryState, BatteryEvent> states)
            throws Exception {
        states
            .withStates()
            .initial(BatteryState.IDLE)
            .states(java.util.EnumSet.allOf(BatteryState.class));
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<BatteryState, BatteryEvent> transitions)
            throws Exception {
        transitions
            .withExternal().source(BatteryState.IDLE).target(BatteryState.CHARGING)
                .event(BatteryEvent.START_CHARGING).and()
            .withExternal().source(BatteryState.CHARGING).target(BatteryState.IDLE)
                .event(BatteryEvent.STOP_CHARGING).and()
            .withExternal().source(BatteryState.IDLE).target(BatteryState.DISCHARGING)
                .event(BatteryEvent.START_DISCHARGING).and()
            .withExternal().source(BatteryState.DISCHARGING).target(BatteryState.IDLE)
                .event(BatteryEvent.STOP_DISCHARGING).and()
            .withExternal().source(BatteryState.DISCHARGING).target(BatteryState.CHARGING)
                .event(BatteryEvent.START_CHARGING).and()
            .withExternal()
                .source(BatteryState.CHARGING)
                .target(BatteryState.DISCHARGING)
                .event(BatteryEvent.START_DISCHARGING).and()
            .withExternal().source(BatteryState.CHARGING).target(BatteryState.FAULT)
                .event(BatteryEvent.FAULT_DETECTED).and()
            .withExternal().source(BatteryState.DISCHARGING).target(BatteryState.FAULT)
                .event(BatteryEvent.FAULT_DETECTED).and()
            .withExternal().source(BatteryState.FAULT).target(BatteryState.IDLE)
                .event(BatteryEvent.FAULT_CLEARED);
    }
}