package com.rentdrive.controller;

import com.rentdrive.dto.request.CreateReviewRequest;
import com.rentdrive.dto.response.ApiResponse;
import com.rentdrive.dto.response.PageResponse;
import com.rentdrive.dto.response.ReviewResponse;
import com.rentdrive.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Avis", description = "Avis bilatéraux post-location — locataire note le bailleur et inversement")
@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(
        summary = "Déposer un avis",
        description = "Permet de laisser un avis sur une réservation terminée (statut COMPLETED uniquement). " +
                      "Deux types possibles : " +
                      "RENTER_TO_OWNER (le locataire note le bailleur — seul le locataire peut l'envoyer) ou " +
                      "OWNER_TO_RENTER (le bailleur note le locataire — seul le bailleur peut l'envoyer). " +
                      "Un seul avis par type par réservation. Note de 1 à 5."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Avis déposé"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Un avis de ce type existe déjà pour cette réservation"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Réservation non terminée ou type d'avis non autorisé pour cet utilisateur")
    })
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

    @Operation(
        summary = "Avis d'un véhicule",
        description = "Retourne la liste paginée des avis laissés sur un véhicule spécifique, " +
                      "triés du plus récent au plus ancien. Endpoint public, aucune authentification requise."
    )
    @GetMapping("/api/v1/vehicles/{id}/reviews")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getVehicleReviews(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(ApiResponse.ok(
                reviewService.getVehicleReviews(id,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))));
    }

    @Operation(
        summary = "Avis d'un store",
        description = "Retourne la liste paginée des avis reçus par un store (toutes réservations confondues), " +
                      "triés du plus récent au plus ancien. Endpoint public, aucune authentification requise."
    )
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
