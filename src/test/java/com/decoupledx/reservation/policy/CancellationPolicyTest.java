package com.decoupledx.reservation.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.decoupledx.reservation.shared.domain.BusinessException;
import com.decoupledx.reservation.policy.domain.model.CancellationPolicy;

class CancellationPolicyTest {

    private static final Instant START = Instant.parse("2026-09-01T18:00:00Z");

    @Test
    void allowsCancellationBeforeDeadline() {
        CancellationPolicy policy = new CancellationPolicy(Duration.ofHours(2));
        assertThat(policy.allowsCancellation(Instant.parse("2026-09-01T15:00:00Z"), START)).isTrue();
    }

    @Test
    void allowsCancellationExactlyAtDeadline() {
        CancellationPolicy policy = new CancellationPolicy(Duration.ofHours(2));
        assertThat(policy.allowsCancellation(Instant.parse("2026-09-01T16:00:00Z"), START)).isTrue();
    }

    @Test
    void rejectsCancellationAfterDeadline() {
        CancellationPolicy policy = new CancellationPolicy(Duration.ofHours(2));
        assertThat(policy.allowsCancellation(Instant.parse("2026-09-01T16:00:01Z"), START)).isFalse();
    }

    @Test
    void longerDeadlineIsStricterForSameReservation() {
        Instant now = Instant.parse("2026-09-01T15:00:00Z");
        assertThat(new CancellationPolicy(Duration.ofHours(2)).allowsCancellation(now, START)).isTrue();
        assertThat(new CancellationPolicy(Duration.ofHours(4)).allowsCancellation(now, START)).isFalse();
    }

    @Test
    void rejectsNegativeDeadline() {
        assertThatThrownBy(() -> new CancellationPolicy(Duration.ofMinutes(-1)))
                .isInstanceOf(BusinessException.class);
    }
}
