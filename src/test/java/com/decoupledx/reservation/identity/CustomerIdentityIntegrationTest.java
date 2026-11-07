package com.decoupledx.reservation.identity;

import static com.decoupledx.reservation.testinfra.JwtSupport.customer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.decoupledx.reservation.identity.adapter.api.CustomerId;
import com.decoupledx.reservation.identity.domain.port.CustomerAccountService;
import com.decoupledx.reservation.reservation.domain.port.ReservationRepository;
import com.decoupledx.reservation.testinfra.PostgresIntegrationTest;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class CustomerIdentityIntegrationTest extends PostgresIntegrationTest {

    private static final String FIELD_1 = "a0000000-0000-0000-0000-000000000101";
    private static final String FIELD_2 = "a0000000-0000-0000-0000-000000000102";
    private static final int DEFAULT_DURATION_MINUTES = 90;

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
        jdbc.update("TRUNCATE recurring_reservations, reservations, customers");
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
                .allMatch(r -> CustomerId.of(r.customerId()).equals(internal));
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
                        .content(createRequest(FIELD_1, startTime)))
                .andExpect(status().isCreated());
    }


    @Test
    void concurrentFirstRequestsOfAFreshSubjectObservetheAbortedTransactionRule() throws Exception {
        // Two parallel FIRST-ever requests of one fresh subject race the
        // account INSERT (2026-11-07 rule): the loser of the provisioning
        // race must surface as the honest 409 (customer-hour overlap with
        // the winner on the second field) — never a Postgres aborted-tx 500.
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch start = new CountDownLatch(1);
            Callable<MvcResult> first = booking(start, "race-subject-1", FIELD_1, "2026-09-05T18:00:00");
            Callable<MvcResult> second = booking(start, "race-subject-1", FIELD_2, "2026-09-05T18:00:00");
            Future<MvcResult> firstResult = pool.submit(first);
            Future<MvcResult> secondResult = pool.submit(second);
            start.countDown();

            int firstStatus = firstResult.get().getResponse().getStatus();
            int secondStatus = secondResult.get().getResponse().getStatus();
            List<Integer> statuses = List.of(firstStatus, secondStatus);
            assertThat(statuses).allSatisfy(s -> assertThat(s).isLessThan(500));
            assertThat(statuses.stream().filter(s -> s == 201).count()).isEqualTo(1);
            assertThat(statuses).contains(409);
        } finally {
            pool.shutdownNow();
        }
    }

    private Callable<MvcResult> booking(CountDownLatch start, String subject, String resourceId,
            String startTime) {
        return () -> {
            start.await();
            return mockMvc.perform(post("/api/reservations")
                            .with(customer(subject))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createRequest(resourceId, startTime)))
                    .andReturn();
        };
    }

    private String createRequest(String resourceId, String startTime) {
        return """
                {"resourceId": "%s", "startTime": "%s", "durationMinutes": %d}
                """.formatted(resourceId, startTime, DEFAULT_DURATION_MINUTES);
    }
}
