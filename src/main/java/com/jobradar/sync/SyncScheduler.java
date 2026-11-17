package com.jobradar.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(SyncScheduler.class);

    private final JobSyncService jobSyncService;

    public SyncScheduler(JobSyncService jobSyncService) {
        this.jobSyncService = jobSyncService;
    }

    @Scheduled(
            initialDelayString = "${jobradar.sync.initial-delay:PT1M}",
            fixedDelayString = "${jobradar.sync.interval:PT6H}")
    public void syncAllCompanies() {
        try {
            jobSyncService.syncAllActiveCompanies();
        } catch (IllegalStateException e) {
            // A manual sync is already running, so this scheduled run can safely be skipped.
            log.info("Scheduled sync skipped: {}", e.getMessage());
        }
    }
}