package com.jobradar.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSavedSearchRequest(
        @NotBlank
        @Size(max = 100)
        String name,

        @Size(max = 200)
        String query,

        @Size(max = 100)
        String country,

        @Size(max = 100)
        String location,

        @NotBlank
        @Email
        @Size(max = 320)
        String email) {
}