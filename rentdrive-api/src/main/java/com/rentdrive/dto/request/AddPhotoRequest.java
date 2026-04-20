package com.rentdrive.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddPhotoRequest(

        @NotBlank @Size(max = 500)
        String url,

        boolean isPrimary,

        Byte orderIndex
) {}
