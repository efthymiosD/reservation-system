package com.decoupledx.reservation.shared.event;

import com.decoupledx.reservation.shared.DomainEvent;
import com.decoupledx.reservation.shared.DomainEventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Spring-backed implementation of the {@link DomainEventPublisher} port — the
 * single infrastructure seam of the event pipeline (mirrors
 * {@code SpringTransactionRunner}). When the Spring Modulith registry starter
 * is active, publications are logged in the enclosing transaction and
 * delivered after commit.
 */
@Component
class SpringDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher publisher;

    SpringDomainEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(DomainEvent event) {
        publisher.publishEvent(event);
    }
}
