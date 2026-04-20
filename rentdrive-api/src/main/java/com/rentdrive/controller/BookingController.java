package com.rentdrive.controller;

import com.rentdrive.dto.request.CancelBookingRequest;
import com.rentdrive.dto.request.CreateBookingRequest;
import com.rentdrive.dto.response.ApiResponse;
import com.rentdrive.dto.response.BookingResponse;
import com.rentdrive.dto.response.BookingSummaryResponse;
import com.rentdrive.dto.response.PageResponse;
import com.rentdrive.enums.BookingStatus;
import com.rentdrive.service.BookingService;
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
 * POST   /api/v1/bookings                      LOCATAIRE
 * GET    /api/v1/bookings/me                   LOCATAIRE
 * GET    /api/v1/bookings/{id}                 LOCATAIRE (own) ou BAILLEUR/AGENCE (their store)
 * PATCH  /api/v1/bookings/{id}/confirm         BAILLEUR/AGENCE
 * PATCH  /api/v1/bookings/{id}/cancel          LOCATAIRE ou BAILLEUR/AGENCE
 * PATCH  /api/v1/bookings/{id}/start           BAILLEUR/AGENCE
 * PATCH  /api/v1/bookings/{id}/complete        BAILLEUR/AGENCE
 * GET    /api/v1/stores/me/bookings            BAILLEUR/AGENCE
 * GET    /api/v1/admin/bookings                ADMIN
 */
@RestController
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    // ── Locataire ────────────────────────────────────────────────────────────

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

    @GetMapping("/api/v1/bookings/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingById(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID id) {

        UUID callerId = UUID.fromString(principal.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(bookingService.getById(callerId, id)));
    }

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

    @PatchMapping("/api/v1/bookings/{id}/confirm")
    @PreAuthorize("hasAnyRole('BAILLEUR', 'AGENCE')")
    public ResponseEntity<ApiResponse<BookingResponse>> confirmBooking(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID id) {

        UUID ownerId = UUID.fromString(principal.getUsername());
        return ResponseEntity.ok(
                ApiResponse.ok("Réservation confirmée.", bookingService.confirm(ownerId, id)));
    }

    @PatchMapping("/api/v1/bookings/{id}/start")
    @PreAuthorize("hasAnyRole('BAILLEUR', 'AGENCE')")
    public ResponseEntity<ApiResponse<BookingResponse>> startBooking(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID id) {

        UUID ownerId = UUID.fromString(principal.getUsername());
        return ResponseEntity.ok(
                ApiResponse.ok("Location démarrée.", bookingService.start(ownerId, id)));
    }

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
