package com.decoupledx.reservation.testinfra;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

public final class JwtSupport {

    private JwtSupport() {
    }

    public static RequestPostProcessor customer(String subject) {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> jwt.subject(subject));
    }

    public static RequestPostProcessor admin(String subject) {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> jwt.subject(subject))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
