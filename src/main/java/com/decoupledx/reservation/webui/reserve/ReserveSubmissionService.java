package com.decoupledx.reservation.webui.reserve;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.decoupledx.reservation.identity.api.CustomerId;
import com.decoupledx.reservation.reservation.api.ReservationInfo;
import com.decoupledx.reservation.reservation.api.ReservationApi;

import lombok.RequiredArgsConstructor;

/**
 * Web submission use case: hands the browser-submitted slot to the reservation
 * domain. The domain revalidates everything (availability, blocks, policies,
 * concurrency); this component adds nothing but the call.
 */
@Component
@RequiredArgsConstructor
class ReserveSubmissionService {

    private final ReservationApi createReservation;

    ReservationInfo submit(UUID resourceId, LocalDate date, LocalTime start, int durationMinutes,
            CustomerId customer) {
        return createReservation.create(resourceId, date.atTime(start), durationMinutes, customer);
    }
}
