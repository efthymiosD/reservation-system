package com.decoupledx.reservation.reservation.adapter.persistence;

import com.decoupledx.reservation.identity.adapter.api.CustomerId;
import com.decoupledx.reservation.reservation.adapter.api.AdminReservationFilter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/**
 * Criteria predicate of the combined admin reservation listing. Criteria, not
 * JPQL string parameters: absent constraints simply produce no predicate —
 * no typed-null SQL parameters (Postgres rejects "is null or"-patterns for
 * untyped nulls).
 */
final class ReservationAdminSpecs {

    private ReservationAdminSpecs() {
    }

    static Specification<ReservationEntity> forFilter(AdminReservationFilter filter) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), filter.status().name()));
            }
            if (!filter.customers().isEmpty()) {
                predicates.add(root.get("customerId").in(customerValues(filter.customers())));
            }
            if (filter.startTimeFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.<Instant>get("startTime"), filter.startTimeFrom()));
            }
            if (filter.startTimeToExclusive() != null) {
                predicates.add(cb.lessThan(
                        root.<Instant>get("startTime"), filter.startTimeToExclusive()));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private static List<String> customerValues(List<CustomerId> customers) {
        return customers.stream().map(CustomerId::value).toList();
    }
}
