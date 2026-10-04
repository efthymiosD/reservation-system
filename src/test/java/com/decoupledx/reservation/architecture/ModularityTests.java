package com.decoupledx.reservation.architecture;

import com.decoupledx.reservation.ReservationApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {

    @Test
    void verifiesModularStructure() {
        ApplicationModules.of(ReservationApplication.class).verify();
    }
}
