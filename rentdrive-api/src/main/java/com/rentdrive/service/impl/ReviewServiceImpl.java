package com.rentdrive.service.impl;

import com.rentdrive.dto.request.CreateReviewRequest;
import com.rentdrive.dto.response.PageResponse;
import com.rentdrive.dto.response.ReviewResponse;
import com.rentdrive.entity.Booking;
import com.rentdrive.entity.Review;
import com.rentdrive.entity.User;
import com.rentdrive.enums.BookingStatus;
import com.rentdrive.enums.ReviewType;
import com.rentdrive.exception.ConflictException;
import com.rentdrive.exception.ForbiddenException;
import com.rentdrive.exception.ResourceNotFoundException;
import com.rentdrive.exception.ValidationException;
import com.rentdrive.mapper.ReviewMapper;
import com.rentdrive.repository.BookingRepository;
import com.rentdrive.repository.ReviewRepository;
import com.rentdrive.repository.UserRepository;
import com.rentdrive.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository  reviewRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository    userRepository;
    private final ReviewMapper      reviewMapper;

    @Override
    @Transactional
    public ReviewResponse create(UUID authorId, CreateReviewRequest req) {
        Booking booking = bookingRepository.findByIdWithDetails(req.bookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", req.bookingId()));

        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new ValidationException(
                    "Un avis ne peut être déposé que sur une réservation terminée.",
                    Map.of("bookingId", "Réservation non terminée"));
        }

        boolean isRenter = booking.getRenter().getId().equals(authorId);
        boolean isOwner  = bookingRepository.existsByIdAndStoreOwnerId(req.bookingId(), authorId);

        if (req.type() == ReviewType.RENTER_TO_OWNER && !isRenter) {
            throw new ForbiddenException("Seul le locataire peut déposer un avis RENTER_TO_OWNER.");
        }
        if (req.type() == ReviewType.OWNER_TO_RENTER && !isOwner) {
            throw new ForbiddenException("Seul le bailleur peut déposer un avis OWNER_TO_RENTER.");
        }

        if (reviewRepository.existsByBookingIdAndType(req.bookingId(), req.type())) {
            throw new ConflictException("Un avis de ce type existe déjà pour cette réservation.");
        }

        User author = userRepository.findByIdWithProfile(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("User", authorId));

        User target = req.type() == ReviewType.RENTER_TO_OWNER
                ? booking.getStore().getOwner()
                : booking.getRenter();

        Review review = new Review();
        review.setBooking(booking);
        review.setAuthor(author);
        review.setTarget(target);
        review.setVehicle(booking.getVehicle());
        review.setRating(req.rating());
        review.setComment(req.comment());
        review.setType(req.type());

        Review saved = reviewRepository.save(review);
        log.info("Avis créé : {} (booking={}, type={})", saved.getId(), req.bookingId(), req.type());

        return reviewMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getVehicleReviews(UUID vehicleId, Pageable pageable) {
        return PageResponse.of(
                reviewRepository.findByVehicleId(vehicleId, pageable)
                                .map(reviewMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getStoreReviews(UUID storeId, Pageable pageable) {
        return PageResponse.of(
                reviewRepository.findByStoreId(storeId, pageable)
                                .map(reviewMapper::toResponse));
    }
}
