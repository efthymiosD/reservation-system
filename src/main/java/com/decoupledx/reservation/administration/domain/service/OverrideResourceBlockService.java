package com.decoupledx.reservation.administration.domain.service;

import java.util.List;

import com.decoupledx.reservation.identity.api.CustomerId;
import com.decoupledx.reservation.reservation.api.ReservationApi;
import com.decoupledx.reservation.reservation.api.ReservationInfo;
import com.decoupledx.reservation.reservation.api.ReservationApi;
import com.decoupledx.reservation.resource.api.CreateBlockCommand;
import com.decoupledx.reservation.resource.api.ResourceBlockInfo;
import com.decoupledx.reservation.resource.api.ResourceApi;
import com.decoupledx.reservation.shared.domain.TransactionRunner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class OverrideResourceBlockService {

    private final ResourceApi resourceService;
    private final ReservationApi reservationApi;
    private final TransactionRunner tx;

    public OverrideResult override(CreateBlockCommand command, CustomerId actor) {
        return tx.run(() -> {
            resourceService.lockResource(command.resourceId().value());

            List<ReservationInfo> conflicts = reservationApi.findActiveOverlappingResource(
                    command.resourceId().value(), command.period());
            conflicts.forEach(conflict -> reservationApi.cancelAdministratively(conflict.id().value(), actor));

            ResourceBlockInfo block = resourceService.createBlock(command);
            log.info("Resource block override resourceId={} cancelledReservations={} actor={}",
                    command.resourceId().value(), conflicts.size(), actor.value());
            return new OverrideResult(block, conflicts.size());
        });
    }

    public record OverrideResult(ResourceBlockInfo block, int cancelledReservations) {
    }
}
