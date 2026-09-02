package com.decoupledx.reservation.shared.domain;

import java.util.function.Supplier;

/**
 * Framework-agnostic transaction boundary for domain services.
 * Implemented by an adapter in the infrastructure layer.
 */
public interface TransactionRunner {

    <T> T run(Supplier<T> work);

    void run(Runnable work);

    <T> T runReadOnly(Supplier<T> work);
}