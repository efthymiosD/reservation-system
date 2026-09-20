package com.decoupledx.reservation.availability.domain.port;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.decoupledx.reservation.availability.adapter.api.AvailableResource;
import com.decoupledx.reservation.availability.adapter.api.ResourceAvailability;

/**
 * Inbound port for the availability module: per-slot availability of every
 * resource (with the backend-computed price) and the free-resource list.
 */
public interface AvailabilityService {

    List<ResourceAvailability> resourceAvailability(LocalDate date, LocalTime startTime, int durationMinutes);

    List<AvailableResource> findAvailable(LocalDate date, LocalTime startTime, int durationMinutes);
}
