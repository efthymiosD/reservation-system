package com.decoupledx.reservation.resource.adapter.out.persistence;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.decoupledx.reservation.identity.domain.model.CustomerId;
import com.decoupledx.reservation.resource.domain.model.BlockStatus;
import com.decoupledx.reservation.resource.domain.model.ResourceBlockId;
import com.decoupledx.reservation.resource.domain.model.ResourceId;
import com.decoupledx.reservation.resource.domain.model.ResourceBlock;
import com.decoupledx.reservation.resource.domain.port.ResourceBlockRepository;
import com.decoupledx.reservation.shared.domain.BusinessException;
import com.decoupledx.reservation.shared.domain.ErrorCode;
import com.decoupledx.reservation.shared.domain.ReservationPeriod;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class ResourceBlockPersistenceAdapter implements ResourceBlockRepository {

    private static final String BLOCK_OVERLAP_CONSTRAINT = "resource_blocks_no_overlap";

    private final ResourceBlockJpaRepository blocks;

    @Override
    public Optional<ResourceBlock> findById(ResourceBlockId id) {
        return blocks.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<ResourceBlock> findByResourceId(ResourceId resourceId) {
        return blocks.findByResourceId(resourceId.value()).stream().map(this::toDomain).toList();
    }

    @Override
    public List<ResourceBlock> findAll() {
        return blocks.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public List<ResourceBlock> findActiveOverlapping(Collection<ResourceId> resourceIds, ReservationPeriod period) {
        List<java.util.UUID> ids = resourceIds.stream().map(ResourceId::value).toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        return blocks.findActiveOverlapping(ids, period.start(), period.end()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public ResourceBlock save(ResourceBlock block) {
        try {
            blocks.findById(block.getId().value())
                    .ifPresentOrElse(
                            entity -> entity.updateFrom(
                                    block.getStatus().name(),
                                    block.getCancelledAt(),
                                    block.getCancelledBy() == null ? null : block.getCancelledBy().value()),
                            () -> blocks.saveAndFlush(toEntity(block)));
            blocks.flush();
            return block;
        } catch (DataIntegrityViolationException exception) {
            if (constraintName(exception).contains(BLOCK_OVERLAP_CONSTRAINT)) {
                throw new BusinessException(ErrorCode.BLOCK_OVERLAPS);
            }
            throw exception;
        }
    }

    private static String constraintName(DataIntegrityViolationException exception) {
        return exception.getMostSpecificCause().getMessage() == null
                ? ""
                : exception.getMostSpecificCause().getMessage();
    }

    private ResourceBlockEntity toEntity(ResourceBlock block) {
        return new ResourceBlockEntity(
                block.getId().value(),
                block.getResourceId().value(),
                block.getPeriod().start(),
                block.getPeriod().end(),
                block.getReason(),
                block.getStatus().name(),
                block.getCreatedAt(),
                block.getCancelledAt(),
                block.getCancelledBy() == null ? null : block.getCancelledBy().value());
    }

    private ResourceBlock toDomain(ResourceBlockEntity entity) {
        return ResourceBlock.reconstitute(
                ResourceBlockId.of(entity.getId()),
                ResourceId.of(entity.getResourceId()),
                ReservationPeriod.of(entity.getStartTime(), entity.getEndTime()),
                entity.getReason(),
                BlockStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getCancelledAt(),
                entity.getCancelledBy() == null ? null : CustomerId.of(entity.getCancelledBy()));
    }
}
