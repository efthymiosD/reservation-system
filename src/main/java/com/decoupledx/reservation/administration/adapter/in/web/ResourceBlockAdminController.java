package com.decoupledx.reservation.administration.adapter.in.web;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.decoupledx.reservation.administration.domain.service.BlockResourceService;
import com.decoupledx.reservation.administration.domain.service.OverrideResourceBlockService;
import com.decoupledx.reservation.identity.adapter.in.CurrentCustomerResolver;
import com.decoupledx.reservation.resource.domain.model.BlockStatus;
import com.decoupledx.reservation.resource.domain.model.CreateBlockCommand;
import com.decoupledx.reservation.resource.domain.model.ResourceBlockId;
import com.decoupledx.reservation.resource.domain.model.ResourceBlockInfo;
import com.decoupledx.reservation.resource.domain.model.ResourceId;
import com.decoupledx.reservation.shared.domain.ReservationPeriod;
import com.decoupledx.reservation.venue.domain.service.VenueService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/resource-blocks")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
class ResourceBlockAdminController {

    private final BlockResourceService blockResource;
    private final OverrideResourceBlockService overrideResourceBlock;
    private final VenueService venueService;
    private final CurrentCustomerResolver currentCustomer;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ResourceBlockResponse block(@Valid @RequestBody BlockRequest request) {
        return toResponse(blockResource.block(toCommand(request)));
    }

    @PostMapping("/override")
    @ResponseStatus(HttpStatus.CREATED)
    OverrideResponse override(@Valid @RequestBody BlockRequest request) {
        OverrideResourceBlockService.OverrideResult result =
                overrideResourceBlock.override(toCommand(request), currentCustomer.currentCustomerId());
        return new OverrideResponse(toResponse(result.block()), result.cancelledReservations());
    }

    @PostMapping("/{blockId}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void cancel(@PathVariable UUID blockId) {
        blockResource.cancelBlock(ResourceBlockId.of(blockId), currentCustomer.currentCustomerId());
    }

    @GetMapping
    List<ResourceBlockResponse> findBlocks(
            @RequestParam(required = false) UUID resourceId,
            @RequestParam(required = false) BlockStatus status) {
        List<ResourceBlockInfo> blocks = resourceId == null
                ? blockResource.findAllBlocks()
                : blockResource.findBlocksByResource(ResourceId.of(resourceId));
        return blocks.stream()
                .filter(block -> status == null || block.status() == status)
                .map(this::toResponse)
                .toList();
    }

    private CreateBlockCommand toCommand(BlockRequest request) {
        ZoneId zone = venueService.getVenue(venueService.singleVenueId()).timezone();
        Instant start = request.startTime().atZone(zone).toInstant();
        Instant end = request.endTime().atZone(zone).toInstant();
        return new CreateBlockCommand(
                ResourceId.of(request.resourceId()),
                ReservationPeriod.of(start, end),
                request.reason());
    }

    private ResourceBlockResponse toResponse(ResourceBlockInfo block) {
        return new ResourceBlockResponse(
                block.id().value(),
                block.resourceId().value(),
                block.period().start(),
                block.period().end(),
                block.reason(),
                block.status().name(),
                block.createdAt(),
                block.cancelledAt(),
                block.cancelledBy() == null ? null : block.cancelledBy().value());
    }

    record BlockRequest(
            @NotNull UUID resourceId,
            @NotNull LocalDateTime startTime,
            @NotNull LocalDateTime endTime,
            @NotBlank String reason) {
    }

    record ResourceBlockResponse(
            UUID id,
            UUID resourceId,
            Instant start,
            Instant end,
            String reason,
            String status,
            Instant createdAt,
            Instant cancelledAt,
            String cancelledBy) {
    }

    record OverrideResponse(ResourceBlockResponse block, int cancelledReservations) {
    }
}
