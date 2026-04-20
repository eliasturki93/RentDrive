package com.rentdrive.controller;

import com.rentdrive.dto.request.CreateReviewRequest;
import com.rentdrive.dto.response.ApiResponse;
import com.rentdrive.dto.response.PageResponse;
import com.rentdrive.dto.response.ReviewResponse;
import com.rentdrive.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * POST   /api/v1/reviews                         LOCATAIRE ou BAILLEUR/AGENCE
 * GET    /api/v1/vehicles/{id}/reviews            PUBLIC
 * GET    /api/v1/stores/{id}/reviews              PUBLIC
 */
@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/api/v1/reviews")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody CreateReviewRequest request) {

        UUID authorId = UUID.fromString(principal.getUsername());
        ReviewResponse review = reviewService.create(authorId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Avis déposé avec succès.", review));
    }

    @GetMapping("/api/v1/vehicles/{id}/reviews")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getVehicleReviews(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(ApiResponse.ok(
                reviewService.getVehicleReviews(id,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))));
    }

    @GetMapping("/api/v1/stores/{id}/reviews")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getStoreReviews(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(ApiResponse.ok(
                reviewService.getStoreReviews(id,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))));
    }
}
