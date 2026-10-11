package com.decoupledx.reservation.shared;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.modulith.events.ApplicationModuleListener;

/**
 * The application-facing event-reaction annotation (ADR-0004): a method of a
 * {@code DomainEventHandler} implementation carries this to react to its
 * event after the publishing transaction commits. Modulith's platform
 * listener annotation is composed exactly once — here — so feature modules
 * never name the event platform directly, while the publication registry
 * keeps its per-consumer granularity. (The bean marker itself stays the
 * plain {@code @Component}: component scanning understands type-level
 * composed bean annotations only.)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ApplicationModuleListener
public @interface DomainEventReaction {
}
