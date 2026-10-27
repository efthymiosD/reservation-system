package com.decoupledx.reservation.reservation.domain.port;

import com.decoupledx.reservation.reservation.adapter.api.BookingRequestStatus;
import com.decoupledx.reservation.reservation.adapter.persistence.BookingRequestDataValue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRequestRepository {

    BookingRequestDataValue save(BookingRequestDataValue request);

    Optional<BookingRequestDataValue> findById(UUID id);

    Optional<BookingRequestDataValue> findBySubmissionKey(String submissionKey);

    List<BookingRequestDataValue> findPage(BookingRequestStatus status, int page, int size);

    long count(BookingRequestStatus status);
}
