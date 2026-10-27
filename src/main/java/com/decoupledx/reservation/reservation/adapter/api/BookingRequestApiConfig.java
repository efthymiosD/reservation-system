package com.decoupledx.reservation.reservation.adapter.api;

import com.decoupledx.reservation.reservation.domain.port.BookingRequestLinkService;
import com.decoupledx.reservation.reservation.domain.port.ManageBookingRequestsService;
import com.decoupledx.reservation.reservation.domain.port.SubmitBookingRequestService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class BookingRequestApiConfig {

    @Bean
    BookingRequestApi bookingRequestApi(SubmitBookingRequestService submitBookingRequestService,
                                        ManageBookingRequestsService manageBookingRequestsService,
                                        BookingRequestLinkService bookingRequestLinkService) {
        return new BookingRequestApiImpl(submitBookingRequestService, manageBookingRequestsService,
                bookingRequestLinkService);
    }
}
