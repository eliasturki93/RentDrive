package com.rentdrive.controller;

import com.rentdrive.dto.request.AddPhotoRequest;
import com.rentdrive.dto.request.CreateVehicleRequest;
import com.rentdrive.dto.request.UpdateVehicleRequest;
import com.rentdrive.dto.response.ApiResponse;
import com.rentdrive.dto.response.PageResponse;
import com.rentdrive.dto.response.VehicleResponse;
import com.rentdrive.dto.response.VehicleSummaryResponse;
import com.rentdrive.enums.FuelType;
import com.rentdrive.enums.Transmission;
import com.rentdrive.enums.VehicleCategory;
import com.rentdrive.enums.VehicleStatus;
import com.rentdrive.service.VehicleService;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * POST   /api/v1/vehicles                        BAILLEUR/AGENCE
 * GET    /api/v1/vehicles/mine                   BAILLEUR/AGENCE
 * GET    /api/v1/vehicles/{id}                   Public
 * PATCH  /api/v1/vehicles/{id}                   BAILLEUR/AGENCE (owner)
 * DELETE /api/v1/vehicles/{id}                   BAILLEUR/AGENCE (owner)
 * POST   /api/v1/vehicles/{id}/photos            BAILLEUR/AGENCE (owner)
 * DELETE /api/v1/vehicles/{id}/photos/{photoId}  BAILLEUR/AGENCE (owner)
 * GET    /api/v1/vehicles                        Public (catalogue AVAILABLE)
 * PATCH  /api/v1/admin/vehicles/{id}/status      ADMIN
 * GET    /api/v1/admin/vehicles                  ADMIN
 */
@RestController
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    // ── Owner ────────────────────────────────────────────────────────────────

    @PostMapping("/api/v1/vehicles")
    @PreAuthorize("hasAnyRole('BAILLEUR', 'AGENCE')")
    public ResponseEntity<ApiResponse<VehicleResponse>> createVehicle(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody CreateVehicleRequest request) {

        UUID ownerId = UUID.fromString(principal.getUsername());
        VehicleResponse vehicle = vehicleService.create(ownerId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Véhicule ajouté. En attente de validation.", vehicle));
    }

    @GetMapping("/api/v1/vehicles/mine")
    @PreAuthorize("hasAnyRole('BAILLEUR', 'AGENCE')")
    public ResponseEntity<ApiResponse<List<VehicleSummaryResponse>>> getMyVehicles(
            @AuthenticationPrincipal UserDetails principal) {

        UUID ownerId = UUID.fromString(principal.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(vehicleService.getMyVehicles(ownerId)));
    }

    @PatchMapping("/api/v1/vehicles/{id}")
    @PreAuthorize("hasAnyRole('BAILLEUR', 'AGENCE')")
    public ResponseEntity<ApiResponse<VehicleResponse>> updateVehicle(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateVehicleRequest request) {

        UUID ownerId = UUID.fromString(principal.getUsername());
        return ResponseEntity.ok(
                ApiResponse.ok("Véhicule mis à jour.", vehicleService.update(ownerId, id, request)));
    }

    @DeleteMapping("/api/v1/vehicles/{id}")
    @PreAuthorize("hasAnyRole('BAILLEUR', 'AGENCE')")
    public ResponseEntity<ApiResponse<Void>> deleteVehicle(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID id) {

        UUID ownerId = UUID.fromString(principal.getUsername());
        vehicleService.delete(ownerId, id);
        return ResponseEntity.ok(ApiResponse.ok("Véhicule supprimé.", null));
    }

    @PostMapping("/api/v1/vehicles/{id}/photos")
    @PreAuthorize("hasAnyRole('BAILLEUR', 'AGENCE')")
    public ResponseEntity<ApiResponse<VehicleResponse>> addPhoto(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID id,
            @Valid @RequestBody AddPhotoRequest request) {

        UUID ownerId = UUID.fromString(principal.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Photo ajoutée.", vehicleService.addPhoto(ownerId, id, request)));
    }

    @DeleteMapping("/api/v1/vehicles/{id}/photos/{photoId}")
    @PreAuthorize("hasAnyRole('BAILLEUR', 'AGENCE')")
    public ResponseEntity<ApiResponse<VehicleResponse>> deletePhoto(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID id,
            @PathVariable UUID photoId) {

        UUID ownerId = UUID.fromString(principal.getUsername());
        return ResponseEntity.ok(
                ApiResponse.ok("Photo supprimée.", vehicleService.deletePhoto(ownerId, id, photoId)));
    }

    // ── Public catalogue ─────────────────────────────────────────────────────

    @GetMapping("/api/v1/vehicles/{id}")
    public ResponseEntity<ApiResponse<VehicleResponse>> getVehicleById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(vehicleService.getById(id)));
    }

    @GetMapping("/api/v1/vehicles")
    public ResponseEntity<ApiResponse<PageResponse<VehicleSummaryResponse>>> searchVehicles(
            @RequestParam(required = false) String          wilaya,
            @RequestParam(required = false) VehicleCategory category,
            @RequestParam(required = false) Transmission    transmission,
            @RequestParam(required = false) FuelType        fuelType,
            @RequestParam(required = false) BigDecimal      minPrice,
            @RequestParam(required = false) BigDecimal      maxPrice,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        PageResponse<VehicleSummaryResponse> result = vehicleService.search(
                wilaya, category, transmission, fuelType, minPrice, maxPrice,
                PageRequest.of(page, size, Sort.by("pricePerDay")));
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // ── Admin ────────────────────────────────────────────────────────────────

    @GetMapping("/api/v1/admin/vehicles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<VehicleSummaryResponse>>> listAllVehicles(
            @RequestParam(required = false) VehicleStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        PageResponse<VehicleSummaryResponse> result = vehicleService.listAll(
                status, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PatchMapping("/api/v1/admin/vehicles/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<VehicleResponse>> updateVehicleStatus(
            @PathVariable UUID id,
            @RequestParam VehicleStatus status) {

        return ResponseEntity.ok(
                ApiResponse.ok("Statut mis à jour.", vehicleService.updateStatus(id, status)));
    }
}
