package com.rentdrive.service.impl;

import com.rentdrive.dto.request.ReviewDocumentRequest;
import com.rentdrive.dto.request.UploadDocumentRequest;
import com.rentdrive.dto.response.DocumentResponse;
import com.rentdrive.dto.response.PageResponse;
import com.rentdrive.entity.Document;
import com.rentdrive.entity.User;
import com.rentdrive.enums.VerifStatus;
import com.rentdrive.exception.ConflictException;
import com.rentdrive.exception.ResourceNotFoundException;
import com.rentdrive.exception.ValidationException;
import com.rentdrive.mapper.DocumentMapper;
import com.rentdrive.repository.DocumentRepository;
import com.rentdrive.repository.UserRepository;
import com.rentdrive.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository     userRepository;
    private final DocumentMapper     documentMapper;

    @Override
    @Transactional
    public DocumentResponse upload(UUID userId, UploadDocumentRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        boolean alreadyVerified = documentRepository.findByUserId(userId).stream()
                .anyMatch(d -> d.getType() == req.type() && d.getStatus() == VerifStatus.VERIFIED);
        if (alreadyVerified) {
            throw new ConflictException("Ce type de document est déjà vérifié.");
        }

        if (req.expiryDate() == null &&
                (req.type().name().equals("NATIONAL_ID") || req.type().name().equals("DRIVER_LICENSE"))) {
            throw new ValidationException(
                    "La date d'expiration est obligatoire pour ce type de document.",
                    Map.of("expiryDate", "Obligatoire pour NATIONAL_ID et DRIVER_LICENSE"));
        }

        Document doc = new Document();
        doc.setUser(user);
        doc.setType(req.type());
        doc.setFileUrl(req.fileUrl());
        doc.setExpiryDate(req.expiryDate());
        doc.setStatus(VerifStatus.PENDING);

        Document saved = documentRepository.save(doc);
        log.info("Document soumis : {} (user={}, type={})", saved.getId(), userId, req.type());

        return documentMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponse> getMyDocuments(UUID userId) {
        return documentRepository.findByUserId(userId)
                .stream()
                .map(documentMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public DocumentResponse review(UUID adminId, UUID documentId, ReviewDocumentRequest req) {
        if (req.status() == VerifStatus.PENDING) {
            throw new ValidationException(
                    "Le statut PENDING ne peut pas être attribué manuellement.",
                    Map.of("status", "Valeurs autorisées : VERIFIED, REJECTED"));
        }
        if (req.status() == VerifStatus.REJECTED && (req.rejectionReason() == null || req.rejectionReason().isBlank())) {
            throw new ValidationException(
                    "Une raison est obligatoire pour un rejet.",
                    Map.of("rejectionReason", "Obligatoire si status = REJECTED"));
        }

        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", documentId));

        if (doc.getStatus() != VerifStatus.PENDING) {
            throw new ConflictException("Ce document a déjà été traité.");
        }

        doc.setStatus(req.status());
        doc.setVerifiedBy(adminId);
        if (req.status() == VerifStatus.REJECTED) {
            doc.setRejectionReason(req.rejectionReason());
        }

        log.info("Document {} {} par admin {}", documentId, req.status(), adminId);
        return documentMapper.toResponse(doc);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DocumentResponse> listAll(VerifStatus status, Pageable pageable) {
        return PageResponse.of(
                documentRepository.findAllForAdmin(status, pageable)
                                  .map(documentMapper::toResponse));
    }
}
