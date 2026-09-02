package com.decoupledx.reservation;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import com.decoupledx.reservation.ReservationApplication;

class ModularityTests {

    @Test
    void verifiesModularStructure() {
        ApplicationModules.of(ReservationApplication.class).verify();
    }
}
