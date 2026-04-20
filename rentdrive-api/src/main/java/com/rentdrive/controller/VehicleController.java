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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Tag(name = "Véhicules", description = "Catalogue de véhicules — ajout par les bailleurs, consultation publique et modération admin")
@RestController
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    // ── Owner ────────────────────────────────────────────────────────────────

    @Operation(
        summary = "Ajouter un véhicule",
        description = "Ajoute un nouveau véhicule au catalogue du store de l'utilisateur connecté. " +
                      "Le store doit être approuvé (statut APPROVED) avant de pouvoir ajouter des véhicules. " +
                      "Le véhicule est créé avec le statut PENDING_REVIEW et doit être validé par un admin."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Véhicule ajouté, en attente de validation"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Store non approuvé"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Store introuvable pour cet utilisateur")
    })
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

    @Operation(
        summary = "Mes véhicules",
        description = "Retourne la liste complète des véhicules du store de l'utilisateur connecté, " +
                      "tous statuts confondus (AVAILABLE, RENTED, PENDING_REVIEW, UNAVAILABLE)."
    )
    @GetMapping("/api/v1/vehicles/mine")
    @PreAuthorize("hasAnyRole('BAILLEUR', 'AGENCE')")
    public ResponseEntity<ApiResponse<List<VehicleSummaryResponse>>> getMyVehicles(
            @AuthenticationPrincipal UserDetails principal) {

        UUID ownerId = UUID.fromString(principal.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(vehicleService.getMyVehicles(ownerId)));
    }

    @Operation(
        summary = "Modifier un véhicule",
        description = "Met à jour les informations d'un véhicule appartenant au store de l'utilisateur connecté. " +
                      "Seuls les champs envoyés sont modifiés (PATCH sémantique). " +
                      "Impossible si le véhicule est en cours de location (statut RENTED)."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Véhicule mis à jour"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Ce véhicule n'appartient pas à votre store"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Véhicule introuvable")
    })
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

    @Operation(
        summary = "Supprimer un véhicule",
        description = "Supprime définitivement un véhicule du catalogue. " +
                      "Impossible si le véhicule est actuellement loué (statut RENTED)."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Véhicule supprimé"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Véhicule en cours de location, suppression impossible")
    })
    @DeleteMapping("/api/v1/vehicles/{id}")
    @PreAuthorize("hasAnyRole('BAILLEUR', 'AGENCE')")
    public ResponseEntity<ApiResponse<Void>> deleteVehicle(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID id) {

        UUID ownerId = UUID.fromString(principal.getUsername());
        vehicleService.delete(ownerId, id);
        return ResponseEntity.ok(ApiResponse.ok("Véhicule supprimé.", null));
    }

    @Operation(
        summary = "Ajouter une photo",
        description = "Ajoute une photo au véhicule (URL MinIO). Maximum 10 photos par véhicule. " +
                      "Si isPrimary=true, cette photo devient la photo principale affichée dans le catalogue " +
                      "et l'ancienne photo principale est déclassée."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Photo ajoutée"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Maximum de 10 photos atteint")
    })
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

    @Operation(
        summary = "Supprimer une photo",
        description = "Supprime une photo spécifique d'un véhicule par son ID de photo."
    )
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

    // ── Catalogue public ─────────────────────────────────────────────────────

    @Operation(
        summary = "Détail d'un véhicule",
        description = "Retourne toutes les informations d'un véhicule disponible : " +
                      "marque, modèle, année, catégorie, transmission, carburant, prix/jour, dépôt, " +
                      "photos et informations du store."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Véhicule trouvé"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Véhicule introuvable")
    })
    @GetMapping("/api/v1/vehicles/{id}")
    public ResponseEntity<ApiResponse<VehicleResponse>> getVehicleById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(vehicleService.getById(id)));
    }

    @Operation(
        summary = "Rechercher des véhicules",
        description = "Catalogue public des véhicules disponibles (statut AVAILABLE uniquement), " +
                      "trié par prix croissant. Filtres optionnels cumulables : " +
                      "wilaya (localisation), category (ECONOMY, SUV, LUXURY...), " +
                      "transmission (MANUAL/AUTOMATIC), fuelType (ESSENCE, DIESEL, HYBRID, ELECTRIC), " +
                      "minPrice et maxPrice (prix par jour en DA)."
    )
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

    @Operation(
        summary = "[Admin] Lister tous les véhicules",
        description = "Retourne la liste paginée de tous les véhicules toutes statuts confondus. " +
                      "Filtre optionnel par statut : PENDING_REVIEW (à valider), AVAILABLE, RENTED, UNAVAILABLE."
    )
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

    @Operation(
        summary = "[Admin] Changer le statut d'un véhicule",
        description = "Valide ou modifie le statut d'un véhicule. " +
                      "AVAILABLE : véhicule approuvé et visible dans le catalogue. " +
                      "PENDING_REVIEW : remis en attente de validation. " +
                      "UNAVAILABLE : masqué temporairement du catalogue par l'admin."
    )
    @PatchMapping("/api/v1/admin/vehicles/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<VehicleResponse>> updateVehicleStatus(
            @PathVariable UUID id,
            @RequestParam VehicleStatus status) {

        return ResponseEntity.ok(
                ApiResponse.ok("Statut mis à jour.", vehicleService.updateStatus(id, status)));
    }
}
