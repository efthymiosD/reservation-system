package com.decoupledx.reservation.reservation.adapter.persistence;

import com.decoupledx.reservation.identity.adapter.api.CustomerId;
import com.decoupledx.reservation.reservation.adapter.api.ReservationId;
import com.decoupledx.reservation.reservation.adapter.api.ReservationStatus;
import com.decoupledx.reservation.reservation.domain.port.ReservationRepository;
import com.decoupledx.reservation.resource.adapter.api.ResourceId;
import com.decoupledx.reservation.shared.BusinessException;
import com.decoupledx.reservation.shared.ErrorCode;
import com.decoupledx.reservation.shared.ReservationPeriod;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ReservationPersistenceAdapter implements ReservationRepository {

    private final ReservationJpaRepository reservations;

    @Override
    public ReservationDataValue save(ReservationDataValue reservation) {
        try {
            reservations.findById(reservation.id()).ifPresentOrElse(
                    entity -> entity.updateFrom(reservation.status(), reservation.cancelledAt(),
                            reservation.cancelledBy()),
                    () -> reservations.save(toEntity(reservation)));
            reservations.flush();
            return reservation;
        } catch (DataIntegrityViolationException | JpaSystemException |
                 org.springframework.orm.ObjectOptimisticLockingFailureException conflict) {
            // The btree_gist exclusion constraints (resource/customer overlap, blocked
            // periods) reject the write under concurrency; translate to the shared
            // 409 business conflict so no lock-race loser surfaces as a 500.
            throw new BusinessException(ErrorCode.RESOURCE_NO_LONGER_AVAILABLE);
        }
    }

    @Override
    public Optional<ReservationDataValue> findById(ReservationId id) {
        return reservations.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<ReservationDataValue> findByCustomer(CustomerId customerId) {
        return reservations.findByCustomerIdOrderByStartTimeDesc(customerId.value()).stream()
                .map(this::toDomain).toList();
    }

    @Override
    public List<ReservationDataValue> findByCustomer(CustomerId customerId, ReservationStatus status,
                                                     int page, int size) {
        var pageRequest = PageRequest.of(page, size);
        var entities = status == null
                ? reservations.findByCustomerIdOrderByStartTimeDesc(customerId.value(), pageRequest)
                : reservations.findByCustomerIdAndStatusOrderByStartTimeDesc(customerId.value(),
                status.name(), pageRequest);
        return entities.stream().map(this::toDomain).toList();
    }

    @Override
    public long countByCustomer(CustomerId customerId, ReservationStatus status) {
        return status == null
                ? reservations.countByCustomerId(customerId.value())
                : reservations.countByCustomerIdAndStatus(customerId.value(), status.name());
    }

    @Override
    public List<ReservationDataValue> findActiveOverlappingResource(ResourceId resourceId,
                                                                    ReservationPeriod period) {
        return reservations.findActiveOverlappingResource(resourceId.value(), period.start(), period.end())
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<ReservationDataValue> findActiveOverlappingCustomer(CustomerId customerId,
                                                                    ReservationPeriod period) {
        return reservations.findActiveOverlappingCustomer(customerId.value(), period.start(), period.end())
                .stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsActiveOverlappingCustomer(CustomerId customerId, ReservationPeriod period) {
        return reservations.existsActiveOverlappingCustomer(customerId.value(), period.start(), period.end());
    }

    @Override
    public List<ReservationDataValue> findActiveByRecurringReservationId(UUID recurringReservationId) {
        return reservations.findActiveByRecurringReservationId(recurringReservationId).stream()
                .map(this::toDomain).toList();
    }

    @Override
    public List<ReservationDataValue> findAll(ReservationStatus status, int page, int size) {
        var pageRequest = PageRequest.of(page, size);
        var entities = status == null
                ? reservations.findAllByOrderByStartTimeDesc(pageRequest)
                : reservations.findByStatusOrderByStartTimeDesc(status.name(), pageRequest);
        return entities.stream().map(this::toDomain).toList();
    }

    @Override
    public long countAll(ReservationStatus status) {
        return status == null ? reservations.count() : reservations.countByStatus(status.name());
    }

    private ReservationEntity toEntity(ReservationDataValue data) {
        return new ReservationEntity(
                data.id(), data.resourceId(), data.customerId(),
                data.startTime(), data.endTime(), data.status(),
                data.priceAmount(), data.priceCurrency(),
                data.createdAt(), data.cancelledAt(), data.cancelledBy(),
                data.recurringReservationId());
    }

    private ReservationDataValue toDomain(ReservationEntity entity) {
        return new ReservationDataValue(
                entity.getId(), entity.getResourceId(), entity.getCustomerId(),
                entity.getStartTime(), entity.getEndTime(), entity.getStatus(),
                entity.getPriceAmount(),
                entity.getPriceCurrency(), entity.getCreatedAt(), entity.getCancelledAt(),
                entity.getCancelledBy(), entity.getRecurringReservationId());
    }
}
