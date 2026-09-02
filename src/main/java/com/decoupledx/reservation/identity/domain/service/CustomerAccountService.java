package com.decoupledx.reservation.identity.domain.service;

import com.decoupledx.reservation.identity.domain.port.CustomerAccountRepository;
import com.decoupledx.reservation.shared.domain.TransactionRunner;
import com.decoupledx.reservation.identity.domain.model.CustomerId;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CustomerAccountService {

    private final CustomerAccountRepository customerAccounts;
    private final TransactionRunner tx;

    public CustomerId resolveOrProvision(String idpSubject) {
        return tx.run(() -> customerAccounts.findBySubject(idpSubject)
                .orElseGet(() -> customerAccounts.create(idpSubject)));
    }

}