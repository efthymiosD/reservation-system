package com.decoupledx.reservation.identity.adapter;

import com.decoupledx.reservation.identity.domain.port.CustomerAccountRepository;
import com.decoupledx.reservation.identity.domain.service.CustomerAccountService;
import com.decoupledx.reservation.shared.TransactionRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Module-internal wiring; nothing here is visible to other modules.
 */
@Configuration
class IdentityModuleConfig {

    @Bean
    CustomerAccountService customerAccountService(CustomerAccountRepository repository, TransactionRunner tx) {
        return new CustomerAccountService(repository, tx);
    }
}
