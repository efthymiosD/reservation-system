package com.decoupledx.reservation.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.decoupledx.reservation.identity.domain.port.CustomerAccountRepository;
import com.decoupledx.reservation.shared.domain.TransactionRunner;
import com.decoupledx.reservation.identity.domain.model.CustomerId;
import com.decoupledx.reservation.identity.domain.service.CustomerAccountService;

class CustomerAccountServiceTest {

    private final StubCustomerAccounts accounts = new StubCustomerAccounts();
    private final CustomerAccountService service = new CustomerAccountService(accounts, synchronousTx());

    @Test
    void resolvesExistingSubjectToItsInternalCustomerId() {
        CustomerId existing = service.resolveOrProvision("idp-1");
        CustomerId again = service.resolveOrProvision("idp-1");

        assertThat(again).isEqualTo(existing);
        assertThat(accounts.provisioned).isEqualTo(1);
    }

    @Test
    void provisionsUnknownSubjectOnceAndReturnsStableId() {
        CustomerId first = service.resolveOrProvision("idp-1");
        CustomerId second = service.resolveOrProvision("idp-1");

        assertThat(first).isNotNull();
        assertThat(second).isEqualTo(first);
    }

    @Test
    void distinctSubjectsMapToDistinctCustomers() {
        CustomerId a = service.resolveOrProvision("idp-a");
        CustomerId b = service.resolveOrProvision("idp-b");

        assertThat(a).isNotEqualTo(b);
    }

    private static TransactionRunner synchronousTx() {
        return new TransactionRunner() {
            @Override public <T> T run(Supplier<T> work) { return work.get(); }
            @Override public void run(Runnable work) { work.run(); }
            @Override public <T> T runReadOnly(Supplier<T> work) { return work.get(); }
        };
    }

    private static final class StubCustomerAccounts implements CustomerAccountRepository {

        private final Map<String, CustomerId> bySubject = new HashMap<>();
        private int provisioned;

        @Override
        public Optional<CustomerId> findBySubject(String idpSubject) {
            return Optional.ofNullable(bySubject.get(idpSubject));
        }

        @Override
        public CustomerId create(String idpSubject) {
            provisioned++;
            CustomerId id = CustomerId.random();
            bySubject.put(idpSubject, id);
            return id;
        }
    }
}
