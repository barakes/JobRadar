package com.jobradar.sync;

// What happened in a single sync of one company.
public record SyncResult(
        String companyName,
        int created,
        int updated,
        int closed) {
}