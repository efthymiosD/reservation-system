package com.decoupledx.reservation.identity.domain.port;

import java.util.Optional;

import com.decoupledx.reservation.identity.api.CustomerId;

public interface CustomerAccountRepository {

    Optional<CustomerId> findBySubject(String idpSubject);

    CustomerId create(String idpSubject);
}
