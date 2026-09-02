package com.decoupledx.reservation.reservation.domain.model;

import java.time.Instant;
import java.util.Objects;

import com.decoupledx.reservation.identity.domain.model.CustomerId;
import com.decoupledx.reservation.policy.domain.model.CancellationPolicy;
import com.decoupledx.reservation.reservation.domain.model.ReservationId;
import com.decoupledx.reservation.reservation.domain.model.ReservationStatus;
import com.decoupledx.reservation.resource.domain.model.ResourceId;
import com.decoupledx.reservation.shared.domain.BusinessException;
import com.decoupledx.reservation.shared.domain.ErrorCode;
import com.decoupledx.reservation.shared.domain.Money;
import com.decoupledx.reservation.shared.domain.ReservationPeriod;

import lombok.Getter;

@Getter
public class Reservation {

    private final ReservationId id;
    private final ResourceId resourceId;
    private final CustomerId customerId;
    private final ReservationPeriod period;
    private final Money price;
    private ReservationStatus status;
    private final Instant createdAt;
    private Instant cancelledAt;
    private CustomerId cancelledBy;

    private Reservation(ReservationId id, ResourceId resourceId, CustomerId customerId,
                        ReservationPeriod period, Money price, ReservationStatus status,
                        Instant createdAt, Instant cancelledAt, CustomerId cancelledBy) {
        this.id = id;
        this.resourceId = resourceId;
        this.customerId = customerId;
        this.period = period;
        this.price = price;
        this.status = status;
        this.createdAt = createdAt;
        this.cancelledAt = cancelledAt;
        this.cancelledBy = cancelledBy;
    }

    public static Reservation create(ResourceId resourceId, CustomerId customerId,
                                     ReservationPeriod period, Money price, Instant now) {
        Objects.requireNonNull(resourceId, "resourceId must not be null");
        Objects.requireNonNull(customerId, "customerId must not be null");
        Objects.requireNonNull(period, "period must not be null");
        Objects.requireNonNull(price, "price must not be null");
        Objects.requireNonNull(now, "now must not be null");
        return new Reservation(ReservationId.random(), resourceId, customerId, period, price,
                ReservationStatus.ACTIVE, now, null, null);
    }

    public static Reservation reconstitute(ReservationId id, ResourceId resourceId, CustomerId customerId,
                                           ReservationPeriod period, Money price, ReservationStatus status,
                                           Instant createdAt, Instant cancelledAt, CustomerId cancelledBy) {
        return new Reservation(id, resourceId, customerId, period, price, status, createdAt, cancelledAt, cancelledBy);
    }

    public void cancel(Instant now, CancellationPolicy currentPolicy) {
        requireActive();
        if (!currentPolicy.allowsCancellation(now, period.start())) {
            throw new BusinessException(ErrorCode.CANCELLATION_DEADLINE_PASSED);
        }
        this.status = ReservationStatus.CANCELLED;
        this.cancelledAt = now;
        this.cancelledBy = this.customerId;
    }

    public void cancelAdministratively(Instant now, CustomerId actor) {
        requireActive();
        this.status = ReservationStatus.CANCELLED;
        this.cancelledAt = now;
        this.cancelledBy = actor;
    }

    public boolean isActive() {
        return status == ReservationStatus.ACTIVE;
    }

    private void requireActive() {
        if (status == ReservationStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.RESERVATION_ALREADY_CANCELLED);
        }
    }
}
