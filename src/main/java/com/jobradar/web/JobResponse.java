package com.jobradar.web;

import com.jobradar.repository.JobSummary;

import java.time.Instant;

public record JobResponse(
        Long id,
        String title,
        String companyName,
        String location,
        String department,
        String url,
        Instant firstSeenAt) {

    public static JobResponse from(JobSummary job) {
        return new JobResponse(
                job.getId(),
                job.getTitle(),
                job.getCompanyName(),
                job.getLocation(),
                job.getDepartment(),
                job.getUrl(),
                job.getFirstSeenAt());
    }
}