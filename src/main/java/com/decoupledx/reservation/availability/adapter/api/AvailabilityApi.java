package com.decoupledx.reservation.availability.adapter.api;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Module API of the availability module: per-slot availability of every
 * resource (with the backend-computed price) and the free-resource list.
 */
public interface AvailabilityApi {

    List<ResourceAvailability> resourceAvailability(LocalDate date, LocalTime startTime, int durationMinutes);

    List<AvailableResource> findAvailable(LocalDate date, LocalTime startTime, int durationMinutes);
}
