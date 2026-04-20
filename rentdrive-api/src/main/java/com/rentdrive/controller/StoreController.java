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

@Tag(name = "Stores", description = "Gestion des agences et bailleurs privés — création, consultation et modération")
@RestController
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    // ── Owner ────────────────────────────────────────────────────────────────

    @Operation(
        summary = "Créer mon store",
        description = "Crée le store (agence ou bailleur privé) de l'utilisateur connecté. " +
                      "Un utilisateur ne peut avoir qu'un seul store. " +
                      "Le store est créé avec le statut PENDING et doit être approuvé par un admin avant d'être visible publiquement."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Store créé, en attente de validation"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "L'utilisateur possède déjà un store")
    })
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

    @Operation(
        summary = "Consulter mon store",
        description = "Retourne les informations complètes du store de l'utilisateur connecté, " +
                      "incluant le statut de validation, les statistiques et les informations du propriétaire."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Store retourné"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Aucun store trouvé pour cet utilisateur")
    })
    @GetMapping("/api/v1/stores/me")
    @PreAuthorize("hasAnyRole('BAILLEUR', 'AGENCE')")
    public ResponseEntity<ApiResponse<StoreResponse>> getMyStore(
            @AuthenticationPrincipal UserDetails principal) {

        UUID ownerId = UUID.fromString(principal.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(storeService.getMyStore(ownerId)));
    }

    @Operation(
        summary = "Mettre à jour mon store",
        description = "Modifie les informations du store : nom, description, logo, téléphone, adresse, wilaya. " +
                      "Seuls les champs envoyés sont modifiés (PATCH sémantique). " +
                      "Le type (AGENCY/PRIVATE) ne peut pas être changé après création."
    )
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

    @Operation(
        summary = "Détail d'un store",
        description = "Retourne les informations publiques d'un store approuvé par son ID : " +
                      "nom, description, localisation, note moyenne, nombre d'avis et informations du propriétaire."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Store trouvé"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Store introuvable")
    })
    @GetMapping("/api/v1/stores/{id}")
    public ResponseEntity<ApiResponse<StoreResponse>> getStoreById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(storeService.getById(id)));
    }

    @Operation(
        summary = "Catalogue des stores",
        description = "Liste paginée des stores approuvés, triés par note décroissante. " +
                      "Filtres optionnels : wilaya (ex: 'Alger') et type (AGENCY ou PRIVATE). " +
                      "Seuls les stores avec statut APPROVED sont retournés."
    )
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

    @Operation(
        summary = "[Admin] Lister tous les stores",
        description = "Retourne la liste paginée de tous les stores toutes statuts confondus. " +
                      "Filtre optionnel par statut : PENDING (à valider), APPROVED, REJECTED, SUSPENDED. " +
                      "Triés par date de création décroissante."
    )
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

    @Operation(
        summary = "[Admin] Changer le statut d'un store",
        description = "Valide, rejette ou suspend un store. " +
                      "APPROVED : le store devient visible publiquement et peut ajouter des véhicules. " +
                      "REJECTED : le store est refusé (le propriétaire peut en créer un nouveau). " +
                      "SUSPENDED : le store est masqué temporairement du catalogue."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Statut mis à jour"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Store introuvable")
    })
    @PatchMapping("/api/v1/admin/stores/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StoreResponse>> updateStoreStatus(
            @PathVariable UUID id,
            @RequestParam StoreStatus status) {

        return ResponseEntity.ok(
                ApiResponse.ok("Statut mis à jour.", storeService.updateStatus(id, status)));
    }
}
