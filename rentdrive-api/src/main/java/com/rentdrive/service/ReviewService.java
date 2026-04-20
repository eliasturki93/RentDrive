package com.rentdrive.service;

import com.rentdrive.dto.request.CreateReviewRequest;
import com.rentdrive.dto.response.PageResponse;
import com.rentdrive.dto.response.ReviewResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ReviewService {

    ReviewResponse create(UUID authorId, CreateReviewRequest request);

    PageResponse<ReviewResponse> getVehicleReviews(UUID vehicleId, Pageable pageable);

    PageResponse<ReviewResponse> getStoreReviews(UUID storeId, Pageable pageable);
}
