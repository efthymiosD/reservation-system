package com.decoupledx.reservation.webui.admin;

import com.decoupledx.reservation.reservation.adapter.api.BookingRequestApi;
import com.decoupledx.reservation.reservation.adapter.api.BookingRequestInfo;
import com.decoupledx.reservation.reservation.adapter.api.BookingRequestLinkInfo;
import com.decoupledx.reservation.reservation.adapter.api.BookingRequestPage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.temporal.ChronoUnit;

/**
 * Assembles the coach's booking-request queue view from the module API: rows
 * in display shape (venue-local date/time split + request age in days) and
 * the current shareable-link state.
 */
@Component
@RequiredArgsConstructor
class BookingRequestsFactory {

    private final BookingRequestApi bookingRequests;
    private final Clock clock;

    BookingRequestsModel build() {
        BookingRequestPage page = bookingRequests.listForAdmin(null, 0, 50);
        BookingRequestLinkInfo activeLink = bookingRequests.activeLink().orElse(null);
        return new BookingRequestsModel(
                page.items().stream().map(this::toRow).toList(),
                page.total(), activeLink);
    }

    private BookingRequestRow toRow(BookingRequestInfo info) {
        long ageDays = ChronoUnit.DAYS.between(
                info.createdAt(), clock.instant());
        return new BookingRequestRow(
                info,
                info.startAt().toLocalDate(),
                info.startAt().toLocalTime(),
                info.endAt().toLocalTime(),
                ageDays);
    }
}
