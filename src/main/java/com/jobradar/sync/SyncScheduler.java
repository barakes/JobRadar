package com.jobradar.sync;

import com.jobradar.model.Company;
import com.jobradar.repository.CompanyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(SyncScheduler.class);

    private final CompanyRepository companyRepository;
    private final JobSyncService jobSyncService;

    public SyncScheduler(CompanyRepository companyRepository, JobSyncService jobSyncService) {
        this.companyRepository = companyRepository;
        this.jobSyncService = jobSyncService;
    }

    @Scheduled(
            initialDelayString = "${jobradar.sync.initial-delay:PT1M}",
            fixedDelayString = "${jobradar.sync.interval:PT6H}")
    public void syncAllCompanies() {
        List<Company> companies = companyRepository.findByActiveTrue();
        log.info("Scheduled sync started for {} companies", companies.size());

        int succeeded = 0;
        int failed = 0;

        for (Company company : companies) {
            try {
                jobSyncService.syncCompany(company.getId());
                succeeded++;
            } catch (RuntimeException e) {
                // One failing company must not stop the others from syncing.
                failed++;
                log.warn("Scheduled sync failed for {} (id {}): {}",
                        company.getName(), company.getId(), e.getMessage());
            }
        }

        log.info("Scheduled sync finished: {} succeeded, {} failed", succeeded, failed);
    }
}