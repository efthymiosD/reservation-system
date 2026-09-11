package com.decoupledx.reservation.administration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.decoupledx.reservation.administration.domain.service.BlockResourceService;
import com.decoupledx.reservation.administration.api.AdministrationApi;
import com.decoupledx.reservation.administration.domain.BlockManagementFacade;
import com.decoupledx.reservation.administration.domain.service.OverrideResourceBlockService;
import com.decoupledx.reservation.reservation.api.ReservationApi;
import com.decoupledx.reservation.resource.api.ResourceApi;
import com.decoupledx.reservation.shared.domain.TransactionRunner;

/** Module-internal wiring; nothing here is visible to other modules. */
@Configuration
public class AdministrationModuleConfig {

    @Bean
    BlockResourceService blockResourceService(ResourceApi resourceService, ReservationApi reservationApi,
            TransactionRunner tx) {
        return new BlockResourceService(resourceService, reservationApi, tx);
    }

    @Bean
    OverrideResourceBlockService overrideResourceBlockService(ResourceApi resourceService,
            ReservationApi reservationApi, TransactionRunner tx) {
        return new OverrideResourceBlockService(resourceService, reservationApi, tx);
    }

    @Bean
    AdministrationApi blockManagementFacade(BlockResourceService blockResourceService,
            OverrideResourceBlockService overrideResourceBlockService) {
        return new BlockManagementFacade(blockResourceService, overrideResourceBlockService);
    }
}
