package com.decoupledx.reservation.identity.adapter.api;

import com.decoupledx.reservation.identity.domain.port.CustomerAccountService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class IdentityApiConfig {

    @Bean
    CustomerDirectoryApi customerDirectoryApi(CustomerAccountService customerAccountService) {
        return new CustomerDirectoryApiImpl(customerAccountService);
    }
}
