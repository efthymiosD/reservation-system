package com.decoupledx.reservation.shared;

/**
 * Outbound port: publishes a domain event to the platform-agnostic in-JVM
 * event mechanism. Publishing happens after the related aggregate state
 * change, inside the business transaction (registered there when the event
 * publication registry is active), and delivery to other modules happens
 * after the transaction commits.
 */
public interface DomainEventPublisher {

    void publish(DomainEvent event);
}
