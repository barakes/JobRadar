package com.jobradar.sync;

import com.jobradar.model.Company;
import com.jobradar.model.Job;
import com.jobradar.model.JobSource;
import com.jobradar.repository.CompanyRepository;
import com.jobradar.repository.JobRepository;
import com.jobradar.source.FetchedJob;
import com.jobradar.source.JobSourceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class JobSyncService {

    private static final Logger log = LoggerFactory.getLogger(JobSyncService.class);

    private final CompanyRepository companyRepository;
    private final JobRepository jobRepository;
    private final TransactionTemplate transactionTemplate;
    private final Map<JobSource, JobSourceClient> clients = new EnumMap<>(JobSource.class);

    public JobSyncService(CompanyRepository companyRepository,
                          JobRepository jobRepository,
                          TransactionTemplate transactionTemplate,
                          List<JobSourceClient> clients) {
        this.companyRepository = companyRepository;
        this.jobRepository = jobRepository;
        this.transactionTemplate = transactionTemplate;
        clients.forEach(client -> this.clients.put(client.source(), client));
    }

    public SyncResult syncCompany(Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company " + companyId + " was not found"));

        JobSourceClient client = clients.get(company.getSource());

        if (client == null) {
            throw new IllegalStateException("No client is registered for source " + company.getSource());
        }

        // The network call happens OUTSIDE the database transaction,
        // so a slow external API never holds a database connection.
        List<FetchedJob> fetchedJobs = client.fetchJobs(company.getBoardToken());

        SyncResult result = transactionTemplate.execute(status -> saveJobs(companyId, fetchedJobs));

        log.info("Synced {}: {} new, {} updated, {} closed",
                result.companyName(), result.created(), result.updated(), result.closed());

        return result;
    }

    private SyncResult saveJobs(Long companyId, List<FetchedJob> fetchedJobs) {
        Company company = companyRepository.findById(companyId).orElseThrow();
        Instant now = Instant.now();

        Map<String, Job> existingJobs = jobRepository.findByCompany(company).stream()
                .collect(Collectors.toMap(Job::getExternalId, Function.identity()));

        Set<String> seenIds = new HashSet<>();
        int created = 0;
        int updated = 0;
        int closed = 0;

        for (FetchedJob fetched : fetchedJobs) {
            // Skip jobs with missing data, and duplicates inside the same response.
            if (fetched.externalId() == null || fetched.title() == null || fetched.url() == null
                    || !seenIds.add(fetched.externalId())) {
                continue;
            }

            Job job = existingJobs.get(fetched.externalId());

            if (job == null) {
                jobRepository.save(new Job(
                        company,
                        truncate(fetched.externalId(), 100),
                        truncate(fetched.title(), 300),
                        truncate(fetched.location(), 300),
                        truncate(fetched.department(), 200),
                        truncate(fetched.url(), 1000),
                        fetched.description(),
                        now));
                created++;
            } else {
                job.refresh(
                        truncate(fetched.title(), 300),
                        truncate(fetched.location(), 300),
                        truncate(fetched.department(), 200),
                        truncate(fetched.url(), 1000),
                        fetched.description(),
                        now);
                updated++;
            }
        }

        // Jobs we had before that no longer appear in the source were closed by the company.
        for (Job job : existingJobs.values()) {
            if (job.isActive() && !seenIds.contains(job.getExternalId())) {
                job.markClosed();
                closed++;
            }
        }

        company.markSynced(now);

        return new SyncResult(company.getName(), created, updated, closed);
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}