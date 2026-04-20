package com.rentdrive.controller;

import com.rentdrive.dto.request.CancelBookingRequest;
import com.rentdrive.dto.request.CreateBookingRequest;
import com.rentdrive.dto.response.ApiResponse;
import com.rentdrive.dto.response.BookingResponse;
import com.rentdrive.dto.response.BookingSummaryResponse;
import com.rentdrive.dto.response.PageResponse;
import com.rentdrive.enums.BookingStatus;
import com.rentdrive.service.BookingService;
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

@Tag(name = "Réservations", description = "Cycle de vie complet d'une réservation : création, confirmation, démarrage, clôture et annulation")
@RestController
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    // ── Locataire ────────────────────────────────────────────────────────────

    @Operation(
        summary = "Créer une réservation",
        description = "Le locataire réserve un véhicule pour une période donnée. " +
                      "Le système vérifie qu'il n'y a pas de chevauchement avec d'autres réservations actives. " +
                      "Le montant total est calculé automatiquement : (prix/jour × jours) + commission plateforme + dépôt de garantie. " +
                      "La commission est de 10% pour un bailleur privé et 14% pour une agence. " +
                      "La réservation démarre avec le statut PENDING, en attente de confirmation du bailleur."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Réservation créée, en attente de confirmation"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Véhicule déjà réservé sur cette période ou non disponible"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dates invalides (endDate doit être après startDate)")
    })
    @PostMapping("/api/v1/bookings")
    @PreAuthorize("hasRole('LOCATAIRE')")
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody CreateBookingRequest request) {

        UUID renterId = UUID.fromString(principal.getUsername());
        BookingResponse booking = bookingService.create(renterId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Réservation créée. En attente de confirmation.", booking));
    }

    @Operation(
        summary = "Mes réservations",
        description = "Retourne la liste paginée des réservations effectuées par le locataire connecté, " +
                      "triées par date de création décroissante (les plus récentes en premier)."
    )
    @GetMapping("/api/v1/bookings/me")
    @PreAuthorize("hasRole('LOCATAIRE')")
    public ResponseEntity<ApiResponse<PageResponse<BookingSummaryResponse>>> getMyBookings(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID renterId = UUID.fromString(principal.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(
                bookingService.getMyBookings(renterId,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))));
    }

    @Operation(
        summary = "Détail d'une réservation",
        description = "Retourne les informations complètes d'une réservation, incluant le détail financier et le statut du paiement. " +
                      "Accessible uniquement par le locataire concerné ou le bailleur dont le store est impliqué."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Réservation retournée"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Accès non autorisé à cette réservation"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Réservation introuvable")
    })
    @GetMapping("/api/v1/bookings/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingById(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID id) {

        UUID callerId = UUID.fromString(principal.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(bookingService.getById(callerId, id)));
    }

    @Operation(
        summary = "Annuler une réservation",
        description = "Le locataire ou le bailleur peut annuler une réservation. " +
                      "Impossible si la location est déjà en cours (IN_PROGRESS) ou terminée (COMPLETED). " +
                      "Le paiement est automatiquement marqué comme remboursé et le dépôt libéré."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Réservation annulée"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Annulation impossible (location en cours ou déjà terminée)")
    })
    @PatchMapping("/api/v1/bookings/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) CancelBookingRequest request) {

        UUID callerId = UUID.fromString(principal.getUsername());
        return ResponseEntity.ok(
                ApiResponse.ok("Réservation annulée.", bookingService.cancel(callerId, id, request)));
    }

    // ── Bailleur / Agence ────────────────────────────────────────────────────

    @Operation(
        summary = "Réservations de mon store",
        description = "Retourne la liste paginée des réservations reçues par le store du bailleur connecté. " +
                      "Filtre optionnel par statut : PENDING, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED."
    )
    @GetMapping("/api/v1/stores/me/bookings")
    @PreAuthorize("hasAnyRole('BAILLEUR', 'AGENCE')")
    public ResponseEntity<ApiResponse<PageResponse<BookingSummaryResponse>>> getStoreBookings(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID ownerId = UUID.fromString(principal.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(
                bookingService.getStoreBookings(ownerId, status,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))));
    }

    @Operation(
        summary = "Confirmer une réservation",
        description = "Le bailleur accepte la demande de réservation. " +
                      "La réservation passe de PENDING à CONFIRMED. " +
                      "Uniquement possible sur une réservation en attente (PENDING)."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Réservation confirmée"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "La réservation n'est pas en statut PENDING")
    })
    @PatchMapping("/api/v1/bookings/{id}/confirm")
    @PreAuthorize("hasAnyRole('BAILLEUR', 'AGENCE')")
    public ResponseEntity<ApiResponse<BookingResponse>> confirmBooking(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID id) {

        UUID ownerId = UUID.fromString(principal.getUsername());
        return ResponseEntity.ok(
                ApiResponse.ok("Réservation confirmée.", bookingService.confirm(ownerId, id)));
    }

    @Operation(
        summary = "Démarrer la location",
        description = "Le bailleur remet le véhicule au locataire. " +
                      "La réservation passe de CONFIRMED à IN_PROGRESS et le véhicule est marqué RENTED. " +
                      "Uniquement possible sur une réservation confirmée (CONFIRMED)."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Location démarrée"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "La réservation n'est pas en statut CONFIRMED")
    })
    @PatchMapping("/api/v1/bookings/{id}/start")
    @PreAuthorize("hasAnyRole('BAILLEUR', 'AGENCE')")
    public ResponseEntity<ApiResponse<BookingResponse>> startBooking(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID id) {

        UUID ownerId = UUID.fromString(principal.getUsername());
        return ResponseEntity.ok(
                ApiResponse.ok("Location démarrée.", bookingService.start(ownerId, id)));
    }

    @Operation(
        summary = "Clôturer la location",
        description = "Le bailleur récupère le véhicule et clôture la location. " +
                      "La réservation passe de IN_PROGRESS à COMPLETED. " +
                      "Le véhicule redevient AVAILABLE, le paiement est marqué COMPLETED et le dépôt de garantie est libéré (RELEASED). " +
                      "Après clôture, le locataire et le bailleur peuvent déposer un avis."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Location terminée"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "La réservation n'est pas en statut IN_PROGRESS")
    })
    @PatchMapping("/api/v1/bookings/{id}/complete")
    @PreAuthorize("hasAnyRole('BAILLEUR', 'AGENCE')")
    public ResponseEntity<ApiResponse<BookingResponse>> completeBooking(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID id) {

        UUID ownerId = UUID.fromString(principal.getUsername());
        return ResponseEntity.ok(
                ApiResponse.ok("Location terminée.", bookingService.complete(ownerId, id)));
    }

    // ── Admin ────────────────────────────────────────────────────────────────

    @Operation(
        summary = "[Admin] Lister toutes les réservations",
        description = "Vue globale de toutes les réservations de la plateforme, triées par date décroissante. " +
                      "Filtre optionnel par statut : PENDING, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED, DISPUTED."
    )
    @GetMapping("/api/v1/admin/bookings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<BookingSummaryResponse>>> listAllBookings(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(ApiResponse.ok(
                bookingService.listAll(status,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))));
    }
}
