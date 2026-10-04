package com.decoupledx.reservation.reservation.domain;

import com.decoupledx.reservation.identity.adapter.api.CustomerId;
import com.decoupledx.reservation.reservation.adapter.api.ReservationId;
import com.decoupledx.reservation.reservation.adapter.api.ReservationInfo;
import com.decoupledx.reservation.reservation.adapter.api.ReservationStatus;
import com.decoupledx.reservation.reservation.adapter.persistence.ReservationDataValue;
import com.decoupledx.reservation.resource.adapter.api.ResourceId;
import com.decoupledx.reservation.shared.Money;
import java.util.Currency;

/**
 * Mapping helpers between the persistence view and the module API view.
 */
final class ReservationMappings {

    private ReservationMappings() {
    }

    static ReservationInfo toInfo(ReservationDataValue data) {
        return new ReservationInfo(
                ReservationId.of(data.id()),
                ResourceId.of(data.resourceId()),
                CustomerId.of(data.customerId()),
                data.startTime(),
                data.endTime(),
                ReservationStatus.valueOf(data.status()),
                Money.of(data.priceAmount(), Currency.getInstance(data.priceCurrency())),
                data.createdAt(),
                data.cancelledAt(),
                data.recurringReservationId());
    }
}
