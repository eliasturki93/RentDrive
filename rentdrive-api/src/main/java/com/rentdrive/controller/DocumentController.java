package com.rentdrive.controller;

import com.rentdrive.dto.request.ReviewDocumentRequest;
import com.rentdrive.dto.request.UploadDocumentRequest;
import com.rentdrive.dto.response.ApiResponse;
import com.rentdrive.dto.response.DocumentResponse;
import com.rentdrive.dto.response.PageResponse;
import com.rentdrive.enums.VerifStatus;
import com.rentdrive.service.DocumentService;
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

import java.util.List;
import java.util.UUID;

@Tag(name = "Documents KYC", description = "Soumission et validation des pièces justificatives (identité, permis, assurance)")
@RestController
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @Operation(
        summary = "Soumettre un document",
        description = "Soumet un document KYC pour vérification. " +
                      "Types acceptés : NATIONAL_ID (carte d'identité), DRIVER_LICENSE (permis de conduire), " +
                      "VEHICLE_REGISTRATION (carte grise), INSURANCE (attestation d'assurance). " +
                      "La date d'expiration est obligatoire pour NATIONAL_ID et DRIVER_LICENSE. " +
                      "Impossible de soumettre un nouveau document si un du même type est déjà VERIFIED. " +
                      "Le document est créé avec le statut PENDING."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Document soumis, en attente de vérification"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Ce type de document est déjà vérifié"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Date d'expiration manquante pour un document obligatoire")
    })
    @PostMapping("/api/v1/documents")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<DocumentResponse>> uploadDocument(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody UploadDocumentRequest request) {

        UUID userId = UUID.fromString(principal.getUsername());
        DocumentResponse doc = documentService.upload(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Document soumis, en attente de vérification.", doc));
    }

    @Operation(
        summary = "Mes documents",
        description = "Retourne la liste de tous les documents KYC soumis par l'utilisateur connecté, " +
                      "avec leur statut de vérification (PENDING, VERIFIED ou REJECTED) et la raison de rejet si applicable."
    )
    @GetMapping("/api/v1/documents/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> getMyDocuments(
            @AuthenticationPrincipal UserDetails principal) {

        UUID userId = UUID.fromString(principal.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(documentService.getMyDocuments(userId)));
    }

    @Operation(
        summary = "[Admin] Valider ou rejeter un document",
        description = "L'admin traite un document en attente. " +
                      "VERIFIED : document accepté, l'utilisateur est validé pour ce type de pièce. " +
                      "REJECTED : document refusé, la raison de rejet est obligatoire et sera visible par l'utilisateur. " +
                      "Le statut PENDING ne peut pas être attribué manuellement. " +
                      "Un document déjà traité (VERIFIED ou REJECTED) ne peut plus être modifié."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Statut mis à jour"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Document déjà traité"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Raison de rejet manquante pour un REJECTED"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Document introuvable")
    })
    @PatchMapping("/api/v1/admin/documents/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DocumentResponse>> reviewDocument(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID id,
            @Valid @RequestBody ReviewDocumentRequest request) {

        UUID adminId = UUID.fromString(principal.getUsername());
        return ResponseEntity.ok(
                ApiResponse.ok("Statut du document mis à jour.", documentService.review(adminId, id, request)));
    }

    @Operation(
        summary = "[Admin] Lister tous les documents",
        description = "Retourne la liste paginée de tous les documents KYC soumis sur la plateforme, " +
                      "triés par date de soumission croissante (les plus anciens en premier pour traiter la file). " +
                      "Filtre optionnel par statut : PENDING (à traiter en priorité), VERIFIED, REJECTED."
    )
    @GetMapping("/api/v1/admin/documents")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<DocumentResponse>>> listAllDocuments(
            @RequestParam(required = false) VerifStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(ApiResponse.ok(
                documentService.listAll(status,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt")))));
    }
}
