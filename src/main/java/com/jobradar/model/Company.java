package com.jobradar.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "companies")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobSource source;

    @Column(nullable = false, length = 100)
    private String boardToken;

    @Column(nullable = false)
    private boolean active = true;

    private Instant lastSyncedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // Required by JPA. Protected, so application code must use the constructor below.
    protected Company() {
    }

    public Company(String name, JobSource source, String boardToken) {
        this.name = name;
        this.source = source;
        this.boardToken = boardToken;
    }

    public void markSynced(Instant syncedAt) {
        this.lastSyncedAt = syncedAt;
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

    public JobSource getSource() {
        return source;
    }

    public String getBoardToken() {
        return boardToken;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getLastSyncedAt() {
        return lastSyncedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}