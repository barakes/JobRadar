package com.jobradar.sync;

import com.jobradar.IntegrationTest;
import com.jobradar.model.Company;
import com.jobradar.model.Job;
import com.jobradar.model.JobSource;
import com.jobradar.repository.CompanyRepository;
import com.jobradar.repository.JobRepository;
import com.jobradar.source.FetchedJob;
import com.jobradar.source.JobSourceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class JobSyncServiceTest extends IntegrationTest {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private final FakeJobSourceClient fakeClient = new FakeJobSourceClient();
    private JobSyncService syncService;
    private Company company;

    @BeforeEach
    void setUp() {
        // Every test starts from an empty database.
        jobRepository.deleteAll();
        companyRepository.deleteAll();

        company = companyRepository.save(new Company("Acme", JobSource.GREENHOUSE, "acme"));
        syncService = new JobSyncService(companyRepository, jobRepository, transactionTemplate, List.of(fakeClient));
    }

    @Test
    void firstSyncCreatesAllJobs() {
        fakeClient.willReturn(
                job("1", "Backend Engineer", "Tel Aviv"),
                job("2", "Data Engineer", "London"));

        SyncResult result = syncService.syncCompany(company.getId());

        assertThat(result).isEqualTo(new SyncResult("Acme", 2, 0, 0));
        assertThat(jobsById()).containsOnlyKeys("1", "2");
        assertThat(jobsById().get("1").getCountry()).isEqualTo("Israel");
        assertThat(jobsById().get("2").getCountry()).isEqualTo("United Kingdom");
    }

    @Test
    void syncingAgainUpdatesInsteadOfDuplicating() {
        fakeClient.willReturn(job("1", "Backend Engineer", "Tel Aviv"));
        syncService.syncCompany(company.getId());

        fakeClient.willReturn(job("1", "Senior Backend Engineer", "Tel Aviv"));
        SyncResult result = syncService.syncCompany(company.getId());

        assertThat(result).isEqualTo(new SyncResult("Acme", 0, 1, 0));
        assertThat(jobRepository.count()).isEqualTo(1);
        assertThat(jobsById().get("1").getTitle()).isEqualTo("Senior Backend Engineer");
    }

    @Test
    void jobsMissingFromTheSourceAreClosed() {
        fakeClient.willReturn(
                job("1", "Backend Engineer", "Tel Aviv"),
                job("2", "Data Engineer", "Tel Aviv"));
        syncService.syncCompany(company.getId());

        fakeClient.willReturn(job("1", "Backend Engineer", "Tel Aviv"));
        SyncResult result = syncService.syncCompany(company.getId());

        assertThat(result).isEqualTo(new SyncResult("Acme", 0, 1, 1));
        assertThat(jobsById().get("1").isActive()).isTrue();
        assertThat(jobsById().get("2").isActive()).isFalse();
    }

    @Test
    void closedJobIsReopenedWhenItAppearsAgain() {
        fakeClient.willReturn(
                job("1", "Backend Engineer", "Tel Aviv"),
                job("2", "Data Engineer", "Tel Aviv"));
        syncService.syncCompany(company.getId());

        fakeClient.willReturn(job("1", "Backend Engineer", "Tel Aviv"));
        syncService.syncCompany(company.getId());

        fakeClient.willReturn(
                job("1", "Backend Engineer", "Tel Aviv"),
                job("2", "Data Engineer", "Tel Aviv"));
        SyncResult result = syncService.syncCompany(company.getId());

        assertThat(result).isEqualTo(new SyncResult("Acme", 0, 2, 0));
        assertThat(jobsById().get("2").isActive()).isTrue();
    }

    @Test
    void invalidAndDuplicateEntriesAreSkipped() {
        fakeClient.willReturn(
                job("1", "Backend Engineer", "Tel Aviv"),
                job("1", "Duplicate of job 1", "Tel Aviv"),
                new FetchedJob("2", null, "Tel Aviv", null, "https://example.com/2", null),
                new FetchedJob(null, "Job without an id", "Tel Aviv", null, "https://example.com/x", null));

        SyncResult result = syncService.syncCompany(company.getId());

        assertThat(result).isEqualTo(new SyncResult("Acme", 1, 0, 0));
        assertThat(jobsById()).containsOnlyKeys("1");
        assertThat(jobsById().get("1").getTitle()).isEqualTo("Backend Engineer");
    }

    @Test
    void marksTheCompanyAsSynced() {
        fakeClient.willReturn(job("1", "Backend Engineer", "Tel Aviv"));

        syncService.syncCompany(company.getId());

        Company reloaded = companyRepository.findById(company.getId()).orElseThrow();
        assertThat(reloaded.getLastSyncedAt()).isNotNull();
    }

    private Map<String, Job> jobsById() {
        return jobRepository.findByCompany(company).stream()
                .collect(Collectors.toMap(Job::getExternalId, Function.identity()));
    }

    private static FetchedJob job(String id, String title, String location) {
        return new FetchedJob(id, title, location, "R&D", "https://example.com/jobs/" + id, "Job description");
    }

    // A source that returns whatever the test tells it to, without touching the network.
    private static class FakeJobSourceClient implements JobSourceClient {

        private List<FetchedJob> jobs = List.of();

        void willReturn(FetchedJob... jobs) {
            this.jobs = List.of(jobs);
        }

        @Override
        public JobSource source() {
            return JobSource.GREENHOUSE;
        }

        @Override
        public List<FetchedJob> fetchJobs(String boardToken) {
            return jobs;
        }
    }
}