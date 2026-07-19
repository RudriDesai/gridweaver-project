package com.gridweaver.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.gridweaver.statemachine.BatteryState;

@SpringBootTest
class BatteryStateServiceTest {

    @Autowired
    private BatteryStateService batteryStateService;

    @Test
    void shouldTransitionToDischarging() {

        BatteryState result =
                batteryStateService.evaluate("NODE-DISCHARGE", 95.0);

        assertEquals(BatteryState.DISCHARGING, result);
    }

    @Test
    void shouldTransitionToCharging() {

        BatteryState result =
                batteryStateService.evaluate("NODE-CHARGE", 5.0);

        assertEquals(BatteryState.CHARGING, result);
    }

    @Test
    void shouldReturnToIdle() {

        batteryStateService.evaluate("NODE-IDLE", 95.0);

        BatteryState result =
                batteryStateService.evaluate("NODE-IDLE", 50.0);

        assertEquals(BatteryState.IDLE, result);
    }

    @Test
    void shouldTransitionToFaultForInvalidReading() {

        BatteryState result =
                batteryStateService.evaluate("NODE-FAULT", -10.0);

        assertEquals(BatteryState.FAULT, result);
    }

    @Test
    void shouldRecoverFromFault() {

        batteryStateService.evaluate("NODE-RECOVER", -10.0);

        BatteryState result =
                batteryStateService.evaluate("NODE-RECOVER", 50.0);

        assertEquals(BatteryState.IDLE, result);
    }
    
    @Test
    void shouldRejectInvalidTransition() {

        // Put node into CHARGING
        batteryStateService.evaluate("NODE-TEST", 10.0);

        // Force another START_CHARGING (already charging)
        BatteryState result =
                batteryStateService.evaluate("NODE-TEST", 5.0);

        assertEquals(BatteryState.CHARGING, result);
    }
    
    @Test
    void shouldStayIdleForNormalLoad() {

        BatteryState result =
                batteryStateService.evaluate("NODE-NORMAL", 50.0);

        assertEquals(BatteryState.IDLE, result);
    }
    
    @Test
    void shouldTransitionFromChargingToDischarging() {

        // IDLE -> CHARGING
        batteryStateService.evaluate("NODE-SPIKE", 5.0);
        assertEquals(
                BatteryState.CHARGING,
                batteryStateService.getCurrentState("NODE-SPIKE")
        );

        // CHARGING -> DISCHARGING
        BatteryState result =
                batteryStateService.evaluate("NODE-SPIKE", 95.0);

        assertEquals(BatteryState.DISCHARGING, result);
    }

}