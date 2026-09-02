package com.decoupledx.reservation.reservation.adapter.out.persistence;

import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.decoupledx.reservation.identity.domain.model.CustomerId;
import com.decoupledx.reservation.reservation.domain.model.ReservationId;
import com.decoupledx.reservation.reservation.domain.model.ReservationStatus;
import com.decoupledx.reservation.reservation.domain.model.Reservation;
import com.decoupledx.reservation.reservation.domain.port.ReservationRepository;
import com.decoupledx.reservation.resource.domain.model.ResourceId;
import com.decoupledx.reservation.shared.domain.BusinessException;
import com.decoupledx.reservation.shared.domain.ErrorCode;
import com.decoupledx.reservation.shared.domain.Money;
import com.decoupledx.reservation.shared.domain.ReservationPeriod;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class ReservationPersistenceAdapter implements ReservationRepository {

    private static final String RESOURCE_OVERLAP_CONSTRAINT = "reservations_no_resource_overlap";
    private static final String CUSTOMER_OVERLAP_CONSTRAINT = "reservations_no_customer_overlap";

    private final ReservationJpaRepository reservations;

    @Override
    public Reservation save(Reservation reservation) {
        try {
            reservations.findById(reservation.getId().value())
                    .ifPresentOrElse(
                            entity -> entity.updateFrom(
                                    reservation.getStatus().name(),
                                    reservation.getCancelledAt(),
                                    reservation.getCancelledBy() == null ? null : reservation.getCancelledBy().value()),
                            () -> reservations.saveAndFlush(toEntity(reservation)));
            reservations.flush();
            return reservation;
        } catch (DataIntegrityViolationException exception) {
            throw translate(exception);
        }
    }

    @Override
    public Optional<Reservation> findById(ReservationId id) {
        return reservations.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<Reservation> findByCustomer(CustomerId customerId) {
        return reservations.findByCustomerIdOrderByStartTimeDesc(customerId.value()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Reservation> findByCustomer(CustomerId customerId, ReservationStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100));
        Page<ReservationEntity> result = status == null
                ? reservations.findByCustomerIdOrderByStartTimeDesc(customerId.value(), pageable)
                : reservations.findByCustomerIdAndStatusOrderByStartTimeDesc(
                        customerId.value(), status.name(), pageable);
        return result.getContent().stream().map(this::toDomain).toList();
    }

    @Override
    public long countByCustomer(CustomerId customerId, ReservationStatus status) {
        return status == null
                ? reservations.countByCustomerId(customerId.value())
                : reservations.countByCustomerIdAndStatus(customerId.value(), status.name());
    }

    @Override
    public List<Reservation> findActiveOverlappingResource(ResourceId resourceId, ReservationPeriod period) {
        return reservations
                .findActiveOverlappingResource(resourceId.value(), period.start(), period.end())
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean existsActiveOverlappingCustomer(CustomerId customerId, ReservationPeriod period) {
        return reservations.existsActiveOverlappingCustomer(customerId.value(), period.start(), period.end());
    }

    @Override
    public List<Reservation> findAll(ReservationStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100));
        Page<ReservationEntity> result = status == null
                ? reservations.findAllByOrderByStartTimeDesc(pageable)
                : reservations.findByStatusOrderByStartTimeDesc(status.name(), pageable);
        return result.getContent().stream().map(this::toDomain).toList();
    }

    @Override
    public long countAll(ReservationStatus status) {
        return status == null ? reservations.count() : reservations.countByStatus(status.name());
    }

    private RuntimeException translate(DataIntegrityViolationException exception) {
        String message = exception.getMostSpecificCause().getMessage();
        if (message != null) {
            if (message.contains(RESOURCE_OVERLAP_CONSTRAINT)) {
                return new BusinessException(ErrorCode.RESOURCE_NO_LONGER_AVAILABLE);
            }
            if (message.contains(CUSTOMER_OVERLAP_CONSTRAINT)) {
                return new BusinessException(ErrorCode.CUSTOMER_HAS_OVERLAPPING_RESERVATION);
            }
        }
        return exception;
    }

    private ReservationEntity toEntity(Reservation reservation) {
        return new ReservationEntity(
                reservation.getId().value(),
                reservation.getResourceId().value(),
                reservation.getCustomerId().value(),
                reservation.getPeriod().start(),
                reservation.getPeriod().end(),
                reservation.getStatus().name(),
                reservation.getPrice().amount(),
                reservation.getPrice().currency().getCurrencyCode(),
                reservation.getCreatedAt(),
                reservation.getCancelledAt(),
                reservation.getCancelledBy() == null ? null : reservation.getCancelledBy().value());
    }

    private Reservation toDomain(ReservationEntity entity) {
        return Reservation.reconstitute(
                ReservationId.of(entity.getId()),
                ResourceId.of(entity.getResourceId()),
                CustomerId.of(entity.getCustomerId()),
                ReservationPeriod.of(entity.getStartTime(), entity.getEndTime()),
                Money.of(entity.getPriceAmount(), Currency.getInstance(entity.getPriceCurrency())),
                ReservationStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getCancelledAt(),
                entity.getCancelledBy() == null ? null : CustomerId.of(entity.getCancelledBy()));
    }
}
