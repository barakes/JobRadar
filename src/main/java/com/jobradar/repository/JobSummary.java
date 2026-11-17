package com.jobradar.repository;

import java.time.Instant;

public interface JobSummary {

    Long getId();

    String getTitle();

    String getCompanyName();

    String getLocation();

    String getDepartment();

    String getUrl();

    Instant getFirstSeenAt();
}