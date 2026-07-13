package com.gridweaver.statemachine;

public enum BatteryEvent {
    START_CHARGING,
    STOP_CHARGING,
    START_DISCHARGING,
    STOP_DISCHARGING,
    FAULT_DETECTED,
    FAULT_CLEARED
}