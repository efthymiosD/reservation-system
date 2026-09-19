package com.decoupledx.reservation.shared.transaction;

import java.util.List;
import java.util.function.Supplier;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.NoRollbackRuleAttribute;
import org.springframework.transaction.interceptor.RuleBasedTransactionAttribute;
import org.springframework.transaction.support.TransactionTemplate;

import com.decoupledx.reservation.shared.TransactionRunner;

/**
 * Spring-backed transaction boundary. Read operations run in a read-only
 * transaction; writes of an enclosing transaction simply join it
 * (PROPAGATION_REQUIRED). A data-integrity violation does not roll back the
 * write transaction so persistence adapters can resolve duplicate-key races
 * (e.g. concurrent customer provisioning) within the same transaction.
 */
@Component
class SpringTransactionRunner implements TransactionRunner {

    private final TransactionTemplate transactionTemplate;
    private final TransactionTemplate readOnlyTransactionTemplate;

    public SpringTransactionRunner(PlatformTransactionManager transactionManager) {
        RuleBasedTransactionAttribute writeAttributes = new RuleBasedTransactionAttribute();
        writeAttributes.setRollbackRules(List.of(new NoRollbackRuleAttribute(DataIntegrityViolationException.class)));
        this.transactionTemplate = new TransactionTemplate(transactionManager, writeAttributes);
        this.readOnlyTransactionTemplate = new TransactionTemplate(transactionManager);
        this.readOnlyTransactionTemplate.setReadOnly(true);
    }

    @Override
    public <T> T run(Supplier<T> work) {
        return transactionTemplate.execute(_ -> work.get());
    }

    @Override
    public void run(Runnable work) {
        transactionTemplate.executeWithoutResult(_ -> work.run());
    }

}