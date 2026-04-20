package com.rentdrive.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateStoreRequest(

        @Size(max = 255)
        String name,

        @Size(max = 2000)
        String description,

        @Size(max = 500)
        String logoUrl,

        @Size(max = 20)
        String phone,

        String address,

        @Size(max = 100)
        String city,

        @Size(max = 100)
        String wilaya
) {}
