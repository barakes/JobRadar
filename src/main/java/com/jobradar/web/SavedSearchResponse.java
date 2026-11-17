package com.jobradar.web;

import com.jobradar.model.SavedSearch;

import java.time.Instant;

public record SavedSearchResponse(
        Long id,
        String name,
        String query,
        String country,
        String location,
        String email,
        boolean active,
        Instant lastNotifiedAt) {

    public static SavedSearchResponse from(SavedSearch search) {
        return new SavedSearchResponse(
                search.getId(),
                search.getName(),
                search.getQuery(),
                search.getCountry(),
                search.getLocation(),
                search.getEmail(),
                search.isActive(),
                search.getLastNotifiedAt());
    }
}