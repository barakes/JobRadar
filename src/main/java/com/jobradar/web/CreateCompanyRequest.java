package com.jobradar.web;

import com.jobradar.model.JobSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCompanyRequest(
        @NotBlank
        @Size(max = 200)
        String name,

        @NotNull
        JobSource source,

        @NotBlank
        @Size(max = 100)
        @Pattern(regexp = "[A-Za-z0-9_-]+", message = "may contain only letters, digits, '-' and '_'")
        String boardToken) {
}