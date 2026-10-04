package com.decoupledx.reservation;

import static org.assertj.core.api.Assertions.assertThat;

import com.decoupledx.reservation.testinfra.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

class ApplicationContextSmokeTest extends PostgresIntegrationTest {

    @Autowired
    ApplicationContext context;

    @Test
    void contextLoads() {
        assertThat(context).isNotNull();
    }
}
