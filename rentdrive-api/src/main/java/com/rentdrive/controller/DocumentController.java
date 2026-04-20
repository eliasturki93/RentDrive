package com.rentdrive.controller;

import com.rentdrive.dto.request.ReviewDocumentRequest;
import com.rentdrive.dto.request.UploadDocumentRequest;
import com.rentdrive.dto.response.ApiResponse;
import com.rentdrive.dto.response.DocumentResponse;
import com.rentdrive.dto.response.PageResponse;
import com.rentdrive.enums.VerifStatus;
import com.rentdrive.service.DocumentService;
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

/**
 * POST    /api/v1/documents                           AUTH
 * GET     /api/v1/documents/me                        AUTH
 * PATCH   /api/v1/admin/documents/{id}/status         ADMIN
 * GET     /api/v1/admin/documents                     ADMIN
 */
@RestController
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

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

    @GetMapping("/api/v1/documents/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> getMyDocuments(
            @AuthenticationPrincipal UserDetails principal) {

        UUID userId = UUID.fromString(principal.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(documentService.getMyDocuments(userId)));
    }

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
