package com.decoupledx.reservation.resource.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.decoupledx.reservation.identity.domain.model.CustomerId;
import com.decoupledx.reservation.resource.domain.model.BlockStatus;
import com.decoupledx.reservation.resource.domain.model.ResourceId;
import com.decoupledx.reservation.shared.domain.BusinessException;
import com.decoupledx.reservation.shared.domain.ErrorCode;
import com.decoupledx.reservation.shared.domain.ReservationPeriod;
import com.decoupledx.reservation.resource.domain.model.ResourceBlock;

class ResourceBlockTest {

    private static final Instant NOW = Instant.parse("2026-08-29T10:00:00Z");

    private ResourceBlock newBlock() {
        return ResourceBlock.create(
                ResourceId.random(),
                ReservationPeriod.ofStartAndDuration(NOW.plus(Duration.ofDays(1)), Duration.ofHours(2)),
                "maintenance",
                NOW);
    }

    @Test
    void createsActiveBlockWithReason() {
        ResourceBlock block = newBlock();
        assertThat(block.getStatus()).isEqualTo(BlockStatus.ACTIVE);
        assertThat(block.getReason()).isEqualTo("maintenance");
        assertThat(block.isActive()).isTrue();
    }

    @Test
    void cancelsActiveBlock() {
        ResourceBlock block = newBlock();
        block.cancel(NOW, CustomerId.of("admin-actor"));
        assertThat(block.getStatus()).isEqualTo(BlockStatus.CANCELLED);
        assertThat(block.isActive()).isFalse();
    }

    @Test
    void rejectsCancellingAlreadyCancelledBlock() {
        ResourceBlock block = newBlock();
        block.cancel(NOW, CustomerId.of("admin-actor"));
        assertThatThrownBy(() -> block.cancel(NOW, CustomerId.of("admin-actor")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).errorCode())
                .isEqualTo(ErrorCode.BLOCK_ALREADY_CANCELLED);
    }

    @Test
    void rejectsMissingArguments() {
        assertThatThrownBy(() -> ResourceBlock.create(
                ResourceId.random(),
                ReservationPeriod.ofStartAndDuration(NOW, Duration.ofHours(1)),
                null,
                NOW)).isInstanceOf(NullPointerException.class);
    }
}
