package com.decoupledx.reservation.identity.domain;

import com.decoupledx.common.transaction.TransactionRunner;
import com.decoupledx.reservation.identity.domain.port.CustomerAccountRepository;
import com.decoupledx.reservation.identity.domain.port.CustomerAccountService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Domain-internal wiring; nothing here is visible to other modules.
 */
@Configuration
class IdentityDomainConfig {

    @Bean
    CustomerAccountService customerAccountService(CustomerAccountRepository repository, TransactionRunner tx) {
        return new CustomerAccountServiceImpl(repository, tx);
    }
}
