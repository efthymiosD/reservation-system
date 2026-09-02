package com.decoupledx.reservation.administration.domain.service;

import java.util.List;

import com.decoupledx.reservation.identity.domain.model.CustomerId;
import com.decoupledx.reservation.reservation.domain.service.CancelReservationService;
import com.decoupledx.reservation.reservation.domain.model.ReservationInfo;
import com.decoupledx.reservation.reservation.domain.service.ReservationQueryService;
import com.decoupledx.reservation.resource.domain.model.CreateBlockCommand;
import com.decoupledx.reservation.resource.domain.model.ResourceBlockInfo;
import com.decoupledx.reservation.resource.domain.service.ResourceService;
import com.decoupledx.reservation.shared.domain.TransactionRunner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class OverrideResourceBlockService {

    private final ResourceService resourceService;
    private final ReservationQueryService reservationQueries;
    private final CancelReservationService cancelReservation;
    private final TransactionRunner tx;

    public OverrideResult override(CreateBlockCommand command, CustomerId actor) {
        return tx.run(() -> {
            resourceService.lockResource(command.resourceId());

            List<ReservationInfo> conflicts = reservationQueries.findActiveOverlappingResource(
                    command.resourceId(), command.period());
            conflicts.forEach(conflict -> cancelReservation.cancelAdministratively(conflict.id(), actor));

            ResourceBlockInfo block = resourceService.createBlock(command);
            log.info("Resource block override resourceId={} cancelledReservations={} actor={}",
                    command.resourceId().value(), conflicts.size(), actor.value());
            return new OverrideResult(block, conflicts.size());
        });
    }

    public record OverrideResult(ResourceBlockInfo block, int cancelledReservations) {
    }
}
