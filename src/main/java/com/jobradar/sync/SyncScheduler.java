package com.jobradar.sync;

import com.jobradar.alert.AlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(SyncScheduler.class);

    private final JobSyncService jobSyncService;
    private final AlertService alertService;

    public SyncScheduler(JobSyncService jobSyncService, AlertService alertService) {
        this.jobSyncService = jobSyncService;
        this.alertService = alertService;
    }

    @Scheduled(
            initialDelayString = "${jobradar.sync.initial-delay:PT1M}",
            fixedDelayString = "${jobradar.sync.interval:PT6H}")
    public void syncAllCompanies() {
        try {
            jobSyncService.syncAllActiveCompanies();
        } catch (IllegalStateException e) {
            // A manual sync is already running, and it will send the alerts itself.
            log.info("Scheduled sync skipped: {}", e.getMessage());
            return;
        }

        alertService.sendAlerts();
    }
}