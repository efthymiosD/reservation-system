package com.decoupledx.reservation.webui.reserve;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.decoupledx.reservation.identity.domain.model.CustomerId;
import com.decoupledx.reservation.reservation.domain.model.ReservationInfo;
import com.decoupledx.reservation.reservation.domain.service.CreateReservationService;
import com.decoupledx.reservation.resource.domain.model.ResourceId;

import lombok.RequiredArgsConstructor;

/**
 * Web submission use case: hands the browser-submitted slot to the reservation
 * domain. The domain revalidates everything (availability, blocks, policies,
 * concurrency); this component adds nothing but the call.
 */
@Component
@RequiredArgsConstructor
class ReserveSubmissionService {

    private final CreateReservationService createReservation;

    ReservationInfo submit(UUID resourceId, LocalDate date, LocalTime start, int durationMinutes,
            CustomerId customer) {
        return createReservation.create(
                ResourceId.of(resourceId), date.atTime(start), durationMinutes, customer);
    }
}
