package com.decoupledx.reservation.identity;

import static com.decoupledx.reservation.testinfra.JwtSupport.customer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.decoupledx.reservation.reservation.domain.port.ReservationRepository;
import com.decoupledx.reservation.testinfra.PostgresIntegrationTest;
import com.decoupledx.reservation.identity.domain.model.CustomerId;
import com.decoupledx.reservation.identity.domain.service.CustomerAccountService;

@AutoConfigureMockMvc
class CustomerIdentityIntegrationTest extends PostgresIntegrationTest {

    private static final String FIELD_1 = "a0000000-0000-0000-0000-000000000101";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    CustomerAccountService customerAccounts;

    @Autowired
    ReservationRepository reservations;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void cleanSharedDataset() {
        jdbc.update("TRUNCATE resource_blocks, reservations, customers");
    }

    @Test
    void provisionedInternalCustomerIdIsStoredAndReusedAcrossRequests() throws Exception {
        String subject = "idp-sub-tests-1";
        String startTime = "2026-09-03T18:00:00";

        createReservation(subject, startTime);

        CustomerId internal = customerAccounts.resolveOrProvision(subject);
        CustomerId again = customerAccounts.resolveOrProvision(subject);

        assertThat(again).isEqualTo(internal);
        assertThat(internal.value())
                .as("internal customer id is a UUID, not the raw IdP subject")
                .doesNotContain(subject);

        assertThat(reservations.findByCustomer(internal))
                .hasSize(1)
                .allMatch(r -> r.getCustomerId().equals(internal));
    }

    @Test
    void distinctSubjectsMapToDistinctInternalCustomers() throws Exception {
        String subjectA = "idp-sub-tests-A";
        String subjectB = "idp-sub-tests-B";

        createReservation(subjectA, "2026-09-03T18:00:00");
        createReservation(subjectB, "2026-09-03T20:00:00");

        CustomerId internalA = customerAccounts.resolveOrProvision(subjectA);
        CustomerId internalB = customerAccounts.resolveOrProvision(subjectB);

        assertThat(internalA).isNotEqualTo(internalB);
    }

    private void createReservation(String subject, String startTime) throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .with(customer(subject))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest(startTime, 90)))
                .andExpect(status().isCreated());
    }

    private String createRequest(String startTime, int durationMinutes) {
        return """
                {"resourceId": "%s", "startTime": "%s", "durationMinutes": %d}
                """.formatted(FIELD_1, startTime, durationMinutes);
    }
}
