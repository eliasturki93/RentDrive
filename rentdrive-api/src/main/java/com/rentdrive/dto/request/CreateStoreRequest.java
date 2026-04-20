package com.rentdrive.dto.request;

import com.rentdrive.enums.StoreType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateStoreRequest(

        @NotBlank @Size(max = 255)
        String name,

        @NotNull
        StoreType type,

        @Size(max = 2000)
        String description,

        @Size(max = 20)
        String phone,

        String address,

        @Size(max = 100)
        String city,

        @NotBlank @Size(max = 100)
        String wilaya
) {}
