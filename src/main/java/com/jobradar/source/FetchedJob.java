package com.jobradar.source;

// A job as it was read from an external source, before it is saved.
// Every source adapter returns this same shape, so the rest of the code does not care where a job came from.
public record FetchedJob(
        String externalId,
        String title,
        String location,
        String department,
        String url,
        String description) {
}