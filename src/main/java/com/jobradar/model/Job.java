package com.jobradar.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "jobs")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(nullable = false, length = 100)
    private String externalId;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(length = 300)
    private String location;

    @Column(length = 200)
    private String department;

    @Column(nullable = false, length = 1000)
    private String url;

    private String description;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private Instant firstSeenAt;

    @Column(nullable = false)
    private Instant lastSeenAt;

    // Required by JPA. Protected, so application code must use the constructor below.
    protected Job() {
    }

    public Job(Company company, String externalId, String title, String location,
               String department, String url, String description, Instant seenAt) {
        this.company = company;
        this.externalId = externalId;
        this.title = title;
        this.location = location;
        this.department = department;
        this.url = url;
        this.description = description;
        this.firstSeenAt = seenAt;
        this.lastSeenAt = seenAt;
    }

    // Called on every sync where the job still appears in the source.
    public void refresh(String title, String location, String department,
                        String url, String description, Instant seenAt) {
        this.title = title;
        this.location = location;
        this.department = department;
        this.url = url;
        this.description = description;
        this.lastSeenAt = seenAt;
        this.active = true;
    }

    // Called when the job no longer appears in the source, meaning the position was closed.
    public void markClosed() {
        this.active = false;
    }

    public Long getId() {
        return id;
    }

    public Company getCompany() {
        return company;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getTitle() {
        return title;
    }

    public String getLocation() {
        return location;
    }

    public String getDepartment() {
        return department;
    }

    public String getUrl() {
        return url;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getFirstSeenAt() {
        return firstSeenAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }
}