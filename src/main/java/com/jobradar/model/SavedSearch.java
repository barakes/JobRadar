package com.jobradar.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "saved_searches")
public class SavedSearch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 200)
    private String query;

    @Column(length = 100)
    private String country;

    @Column(length = 100)
    private String location;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(nullable = false)
    private boolean active = true;

    // Only jobs first seen after this moment are sent, so the same job is never sent twice.
    @Column(nullable = false)
    private Instant lastNotifiedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    // Required by JPA. Protected, so application code must use the constructor below.
    protected SavedSearch() {
    }

    public SavedSearch(String name, String query, String country, String location,
                       String email, Instant now) {
        this.name = name;
        this.query = query;
        this.country = country;
        this.location = location;
        this.email = email;
        // Starting from "now" means a new saved search doesn't send all the old jobs.
        this.lastNotifiedAt = now;
        this.createdAt = now;
    }

    public void markNotified(Instant notifiedAt) {
        this.lastNotifiedAt = notifiedAt;
    }

    public void deactivate() {
        this.active = false;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getQuery() {
        return query;
    }

    public String getCountry() {
        return country;
    }

    public String getLocation() {
        return location;
    }

    public String getEmail() {
        return email;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getLastNotifiedAt() {
        return lastNotifiedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}