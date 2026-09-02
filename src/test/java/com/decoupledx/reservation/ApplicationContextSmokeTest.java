package com.decoupledx.reservation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.decoupledx.reservation.testinfra.PostgresIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationContextSmokeTest extends PostgresIntegrationTest {

    @Autowired
    ApplicationContext context;

    @Test
    void contextLoads() {
        assertThat(context).isNotNull();
    }
}
