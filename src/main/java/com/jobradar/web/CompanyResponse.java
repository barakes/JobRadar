package com.jobradar.web;

import com.jobradar.model.Company;
import com.jobradar.model.JobSource;

import java.time.Instant;

public record CompanyResponse(
        Long id,
        String name,
        JobSource source,
        String boardToken,
        boolean active,
        Instant lastSyncedAt) {

    public static CompanyResponse from(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getSource(),
                company.getBoardToken(),
                company.isActive(),
                company.getLastSyncedAt());
    }
}