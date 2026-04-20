package com.rentdrive.service;

import com.rentdrive.dto.request.CancelBookingRequest;
import com.rentdrive.dto.request.CreateBookingRequest;
import com.rentdrive.dto.response.BookingResponse;
import com.rentdrive.dto.response.BookingSummaryResponse;
import com.rentdrive.dto.response.PageResponse;
import com.rentdrive.enums.BookingStatus;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface BookingService {

    BookingResponse create(UUID renterId, CreateBookingRequest request);

    BookingResponse getById(UUID callerId, UUID bookingId);

    PageResponse<BookingSummaryResponse> getMyBookings(UUID renterId, Pageable pageable);

    PageResponse<BookingSummaryResponse> getStoreBookings(UUID ownerId, BookingStatus status, Pageable pageable);

    BookingResponse confirm(UUID ownerId, UUID bookingId);

    BookingResponse cancel(UUID callerId, UUID bookingId, CancelBookingRequest request);

    BookingResponse start(UUID ownerId, UUID bookingId);

    BookingResponse complete(UUID ownerId, UUID bookingId);

    // Admin
    PageResponse<BookingSummaryResponse> listAll(BookingStatus status, Pageable pageable);
}
