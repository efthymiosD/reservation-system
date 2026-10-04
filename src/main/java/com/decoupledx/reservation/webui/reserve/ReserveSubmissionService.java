package com.decoupledx.reservation.webui.reserve;

import com.decoupledx.reservation.reservation.adapter.api.ReservationApi;
import com.decoupledx.reservation.reservation.adapter.api.ReservationInfo;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Web submission use case: hands the browser-submitted slot to the reservation
 * domain. The domain revalidates everything (availability, policies,
 * concurrency); this component adds nothing but the call.
 */
@Component
@RequiredArgsConstructor
class ReserveSubmissionService {

    private final ReservationApi createReservation;

    ReservationInfo submit(UUID resourceId, LocalDate date, LocalTime start, int durationMinutes) {
        return createReservation.create(resourceId, date.atTime(start), durationMinutes);
    }
}
