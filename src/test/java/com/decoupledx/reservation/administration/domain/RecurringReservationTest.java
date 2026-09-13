package com.decoupledx.reservation.administration.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.decoupledx.reservation.administration.api.RecurringReservationStatus;
import com.decoupledx.reservation.administration.domain.model.RecurringReservation;
import com.decoupledx.reservation.identity.api.CustomerId;
import com.decoupledx.reservation.shared.domain.BusinessException;
import com.decoupledx.reservation.shared.domain.ErrorCode;

class RecurringReservationTest {

    private static final UUID RESOURCE = UUID.fromString("a0000000-0000-0000-0000-000000000101");
    private static final CustomerId CUSTOMER = CustomerId.of("11111111-1111-1111-1111-111111111111");
    private static final Instant NOW = Instant.parse("2026-09-01T10:00:00Z");
    private static final LocalDate NEXT_OCCURRENCE = LocalDate.of(2026, 9, 2);

    private RecurringReservation newRecurringReservation() {
        return RecurringReservation.create(
                RESOURCE, CUSTOMER, DayOfWeek.WEDNESDAY,
                LocalTime.of(18, 0), LocalTime.of(19, 30), 3, NEXT_OCCURRENCE, NOW);
    }

    @Test
    void createsActiveRecurringReservationWithGivenCursorAndTimes() {
        RecurringReservation recurringReservation = newRecurringReservation();

        assertThat(recurringReservation.getStatus()).isEqualTo(RecurringReservationStatus.ACTIVE);
        assertThat(recurringReservation.isActive()).isTrue();
        assertThat(recurringReservation.getResourceId()).isEqualTo(RESOURCE);
        assertThat(recurringReservation.getCustomerId()).isEqualTo(CUSTOMER);
        assertThat(recurringReservation.getWeekday()).isEqualTo(DayOfWeek.WEDNESDAY);
        assertThat(recurringReservation.getWindowMonths()).isEqualTo(3);
        assertThat(recurringReservation.getNextOccurrence()).isEqualTo(NEXT_OCCURRENCE);
        assertThat(recurringReservation.getCreatedAt()).isEqualTo(NOW);
        assertThat(recurringReservation.getCancelledAt()).isNull();
        assertThat(recurringReservation.getCancelledBy()).isNull();
    }

    @Test
    void durationMinutesCoversTheReservedPeriod() {
        RecurringReservation recurringReservation = newRecurringReservation();
        assertThat(recurringReservation.durationMinutes()).isEqualTo(90);
    }

    @Test
    void cancelMarksTheRecurringReservationCancelledWithActorAndTimestamp() {
        RecurringReservation recurringReservation = newRecurringReservation();
        CustomerId admin = CustomerId.of("22222222-2222-2222-2222-222222222222");

        recurringReservation.cancel(NOW, admin);

        assertThat(recurringReservation.getStatus()).isEqualTo(RecurringReservationStatus.CANCELLED);
        assertThat(recurringReservation.isActive()).isFalse();
        assertThat(recurringReservation.getCancelledAt()).isEqualTo(NOW);
        assertThat(recurringReservation.getCancelledBy()).isEqualTo(admin);
    }

    @Test
    void rejectsCancellingAnAlreadyCancelledRecurringReservation() {
        RecurringReservation recurringReservation = newRecurringReservation();
        recurringReservation.cancel(NOW, CUSTOMER);

        assertThatThrownBy(() -> recurringReservation.cancel(NOW, CUSTOMER))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).errorCode())
                .isEqualTo(ErrorCode.RECURRING_RESERVATION_ALREADY_CANCELLED);
    }

    @Test
    void advanceCursorOnlyMovesForward() {
        RecurringReservation recurringReservation = newRecurringReservation();
        LocalDate later = NEXT_OCCURRENCE.plusWeeks(1);

        recurringReservation.advanceCursor(later);
        assertThat(recurringReservation.getNextOccurrence()).isEqualTo(later);

        recurringReservation.advanceCursor(NEXT_OCCURRENCE);
        assertThat(recurringReservation.getNextOccurrence()).isEqualTo(later);
    }

    @Test
    void reconstituteRestoresAllState() {
        CustomerId admin = CustomerId.of("22222222-2222-2222-2222-222222222222");
        RecurringReservation original = newRecurringReservation();
        original.cancel(NOW, admin);

        RecurringReservation restored = RecurringReservation.reconstitute(
                original.getId(), original.getResourceId(), original.getCustomerId(),
                original.getWeekday(), original.getStartTime(), original.getEndTime(),
                original.getWindowMonths(), original.getStatus(), original.getNextOccurrence(),
                original.getCreatedAt(), original.getCancelledAt(), original.getCancelledBy());

        assertThat(restored.getId()).isEqualTo(original.getId());
        assertThat(restored.getWindowMonths()).isEqualTo(3);
        assertThat(restored.getStatus()).isEqualTo(RecurringReservationStatus.CANCELLED);
        assertThat(restored.getCancelledBy()).isEqualTo(admin);
        assertThat(restored.getCancelledAt()).isEqualTo(NOW);
    }

    @Test
    void rejectsUndefinedBookingWindows() {
        assertThatThrownBy(() -> RecurringReservation.create(
                RESOURCE, CUSTOMER, DayOfWeek.MONDAY,
                LocalTime.of(18, 0), LocalTime.of(19, 0), 2, NEXT_OCCURRENCE, NOW))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).errorCode())
                .isEqualTo(ErrorCode.INVALID_RECURRING_RESERVATION_WINDOW);

        assertThatThrownBy(() -> RecurringReservation.create(
                RESOURCE, CUSTOMER, DayOfWeek.MONDAY,
                LocalTime.of(18, 0), LocalTime.of(19, 0), 12, NEXT_OCCURRENCE, NOW))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).errorCode())
                .isEqualTo(ErrorCode.INVALID_RECURRING_RESERVATION_WINDOW);
    }
}