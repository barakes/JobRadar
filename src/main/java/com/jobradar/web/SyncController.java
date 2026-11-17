package com.jobradar.web;

import com.jobradar.alert.AlertService;
import com.jobradar.sync.JobSyncService;
import com.jobradar.sync.SyncResult;
import com.jobradar.sync.SyncSummary;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/sync")
public class SyncController {

    private final JobSyncService jobSyncService;
    private final AlertService alertService;

    public SyncController(JobSyncService jobSyncService, AlertService alertService) {
        this.jobSyncService = jobSyncService;
        this.alertService = alertService;
    }

    @PostMapping
    public SyncResponse syncAllCompanies() {
        SyncSummary summary;
        try {
            summary = jobSyncService.syncAllActiveCompanies();
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
        }

        int alertsSent = alertService.sendAlerts();

        return new SyncResponse(summary.succeeded(), summary.failed(), summary.results(), alertsSent);
    }

    public record SyncResponse(int succeeded, int failed, List<SyncResult> results, int alertsSent) {
    }
}