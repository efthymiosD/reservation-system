package com.decoupledx.reservation.identity.adapter.in;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import com.decoupledx.reservation.identity.domain.model.CustomerId;
import com.decoupledx.reservation.identity.domain.service.CustomerAccountService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CurrentCustomerResolver {

    private final CustomerAccountService customerAccounts;

    public CustomerId currentCustomerId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwt) {
            String subject = jwt.getName();
            if (subject != null && !subject.isBlank()) {
                return customerAccounts.resolveOrProvision(subject);
            }
            throw new IllegalStateException("JWT is missing the 'sub' claim required for customer identity");
        }
        throw new IllegalStateException("No authenticated JWT principal present");
    }
}
