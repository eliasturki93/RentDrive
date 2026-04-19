package com.rentdrive.dto.request;

import com.rentdrive.enums.Gender;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** PATCH /api/v1/users/{id}/profile */
public record UpdateProfileRequest(
    @Size(min = 2, max = 100) String firstName,
    @Size(min = 2, max = 100) String lastName,
    @Past(message = "La date de naissance doit être dans le passé") LocalDate dateOfBirth,
    Gender gender,
    @Size(max = 100) String wilaya,
    @Size(max = 100) String city,
    @Size(max = 500) String address,
    @Size(max = 500) String avatarUrl
) {}
