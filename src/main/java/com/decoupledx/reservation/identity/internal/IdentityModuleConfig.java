package com.decoupledx.reservation.identity.internal;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.decoupledx.reservation.identity.domain.service.CustomerAccountService;
import com.decoupledx.reservation.shared.domain.TransactionRunner;
import com.decoupledx.reservation.identity.domain.port.CustomerAccountRepository;

/** Module-internal wiring; nothing here is visible to other modules. */
@Configuration
public class IdentityModuleConfig {

    @Bean
    CustomerAccountService customerAccountService(CustomerAccountRepository repository, TransactionRunner tx) {
        return new CustomerAccountService(repository, tx);
    }
}
