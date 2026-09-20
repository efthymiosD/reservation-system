package com.decoupledx.reservation.availability.adapter.api;

import java.util.List;

import com.decoupledx.reservation.availability.domain.port.AvailabilityService;
import lombok.RequiredArgsConstructor;

/**
 * Module external-facing facade implementing {@link AvailabilityApi} on top of the
 * availability inbound port. The only bean other modules use for this module.
 */
@RequiredArgsConstructor
class AvailabilityApiImpl implements AvailabilityApi {

    private final AvailabilityService availabilityService;

    @Override
    public List<ResourceAvailability> resourceAvailability(java.time.LocalDate date,
            java.time.LocalTime startTime, int durationMinutes) {
        return availabilityService.resourceAvailability(date, startTime, durationMinutes);
    }

    @Override
    public List<AvailableResource> findAvailable(java.time.LocalDate date,
            java.time.LocalTime startTime, int durationMinutes) {
        return availabilityService.findAvailable(date, startTime, durationMinutes);
    }
}
