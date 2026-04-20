package com.rentdrive.mapper;

import com.rentdrive.dto.response.DocumentResponse;
import com.rentdrive.entity.Document;
import org.springframework.stereotype.Component;

@Component
public class DocumentMapper {

    public DocumentResponse toResponse(Document d) {
        return new DocumentResponse(
                d.getId(),
                d.getType(),
                d.getFileUrl(),
                d.getStatus(),
                d.getRejectionReason(),
                d.getExpiryDate(),
                d.getCreatedAt(),
                d.getUpdatedAt());
    }
}
