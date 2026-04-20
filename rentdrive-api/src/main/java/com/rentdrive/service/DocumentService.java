package com.rentdrive.service;

import com.rentdrive.dto.request.ReviewDocumentRequest;
import com.rentdrive.dto.request.UploadDocumentRequest;
import com.rentdrive.dto.response.DocumentResponse;
import com.rentdrive.dto.response.PageResponse;
import com.rentdrive.enums.VerifStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface DocumentService {

    DocumentResponse upload(UUID userId, UploadDocumentRequest request);

    List<DocumentResponse> getMyDocuments(UUID userId);

    DocumentResponse review(UUID adminId, UUID documentId, ReviewDocumentRequest request);

    PageResponse<DocumentResponse> listAll(VerifStatus status, Pageable pageable);
}
