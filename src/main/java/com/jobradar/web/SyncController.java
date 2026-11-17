package com.jobradar.web;

import com.jobradar.sync.JobSyncService;
import com.jobradar.sync.SyncSummary;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/sync")
public class SyncController {

    private final JobSyncService jobSyncService;

    public SyncController(JobSyncService jobSyncService) {
        this.jobSyncService = jobSyncService;
    }

    @PostMapping
    public SyncSummary syncAllCompanies() {
        try {
            return jobSyncService.syncAllActiveCompanies();
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
        }
    }
}