package com.rentdrive.controller;

import com.rentdrive.dto.request.CreateStoreRequest;
import com.rentdrive.dto.request.UpdateStoreRequest;
import com.rentdrive.dto.response.ApiResponse;
import com.rentdrive.dto.response.PageResponse;
import com.rentdrive.dto.response.StoreResponse;
import com.rentdrive.dto.response.StoreSummaryResponse;
import com.rentdrive.enums.StoreStatus;
import com.rentdrive.enums.StoreType;
import com.rentdrive.service.StoreService;
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
 * POST   /api/v1/stores                       BAILLEUR ou AGENCE
 * GET    /api/v1/stores/me                    BAILLEUR ou AGENCE (owner)
 * PATCH  /api/v1/stores/me                    BAILLEUR ou AGENCE (owner)
 * GET    /api/v1/stores/{id}                  Public
 * GET    /api/v1/stores                       Public (APPROVED uniquement)
 * PATCH  /api/v1/admin/stores/{id}/status     ADMIN
 * GET    /api/v1/admin/stores                 ADMIN
 */
@RestController
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    // ── Owner ────────────────────────────────────────────────────────────────

    @PostMapping("/api/v1/stores")
    @PreAuthorize("hasAnyRole('BAILLEUR', 'AGENCE')")
    public ResponseEntity<ApiResponse<StoreResponse>> createStore(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody CreateStoreRequest request) {

        UUID ownerId = UUID.fromString(principal.getUsername());
        StoreResponse store = storeService.createStore(ownerId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Store créé. En attente de validation.", store));
    }

    @GetMapping("/api/v1/stores/me")
    @PreAuthorize("hasAnyRole('BAILLEUR', 'AGENCE')")
    public ResponseEntity<ApiResponse<StoreResponse>> getMyStore(
            @AuthenticationPrincipal UserDetails principal) {

        UUID ownerId = UUID.fromString(principal.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(storeService.getMyStore(ownerId)));
    }

    @PatchMapping("/api/v1/stores/me")
    @PreAuthorize("hasAnyRole('BAILLEUR', 'AGENCE')")
    public ResponseEntity<ApiResponse<StoreResponse>> updateMyStore(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody UpdateStoreRequest request) {

        UUID ownerId = UUID.fromString(principal.getUsername());
        return ResponseEntity.ok(
                ApiResponse.ok("Store mis à jour.", storeService.updateMyStore(ownerId, request)));
    }

    // ── Public ───────────────────────────────────────────────────────────────

    @GetMapping("/api/v1/stores/{id}")
    public ResponseEntity<ApiResponse<StoreResponse>> getStoreById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(storeService.getById(id)));
    }

    @GetMapping("/api/v1/stores")
    public ResponseEntity<ApiResponse<PageResponse<StoreSummaryResponse>>> listStores(
            @RequestParam(required = false) String wilaya,
            @RequestParam(required = false) StoreType type,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        PageResponse<StoreSummaryResponse> result = storeService.listApproved(
                wilaya, type, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "rating")));
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // ── Admin ────────────────────────────────────────────────────────────────

    @GetMapping("/api/v1/admin/stores")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<StoreSummaryResponse>>> listAllStores(
            @RequestParam(required = false) StoreStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        PageResponse<StoreSummaryResponse> result = storeService.listAll(
                status, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PatchMapping("/api/v1/admin/stores/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StoreResponse>> updateStoreStatus(
            @PathVariable UUID id,
            @RequestParam StoreStatus status) {

        return ResponseEntity.ok(
                ApiResponse.ok("Statut mis à jour.", storeService.updateStatus(id, status)));
    }
}
