package com.rentdrive.dto.response;

import com.rentdrive.enums.Gender;

import java.time.LocalDate;
import java.util.UUID; /** Données personnelles du profil. */
public record ProfileResponse(
    UUID      id,
    String    firstName,
    String    lastName,
    String    fullName,
    LocalDate dateOfBirth,
    Gender    gender,
    String    avatarUrl,
    String    wilaya,
    String    city,
    String    address
) {}
