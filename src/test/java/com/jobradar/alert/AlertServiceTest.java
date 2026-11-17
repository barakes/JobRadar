package com.jobradar.alert;

import com.jobradar.IntegrationTest;
import com.jobradar.model.Company;
import com.jobradar.model.Job;
import com.jobradar.model.JobSource;
import com.jobradar.model.SavedSearch;
import com.jobradar.repository.CompanyRepository;
import com.jobradar.repository.JobRepository;
import com.jobradar.repository.SavedSearchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AlertServiceTest extends IntegrationTest {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private SavedSearchRepository savedSearchRepository;

    private final FakeNotifier notifier = new FakeNotifier();
    private AlertService alertService;
    private Company company;

    // The saved search was created a day ago; jobs are placed before or after that moment.
    private final Instant searchCreatedAt = Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MICROS);

    @BeforeEach
    void setUp() {
        savedSearchRepository.deleteAll();
        jobRepository.deleteAll();
        companyRepository.deleteAll();

        company = companyRepository.save(new Company("Acme", JobSource.GREENHOUSE, "acme"));
        alertService = new AlertService(savedSearchRepository, jobRepository, notifier);
    }

    @Test
    void sendsOnlyNewJobsThatMatchTheSearch() {
        saveSearch("Jobs in Israel", null, "israel");
        saveJob("1", "Backend Engineer", "Tel Aviv", "https://example.com/1", 1);   // new, matches
        saveJob("2", "Data Engineer", "London", "https://example.com/2", 1);        // new, other country
        saveJob("3", "Old Engineer", "Tel Aviv", "https://example.com/3", -1);      // before the search existed

        int sent = alertService.sendAlerts();

        assertThat(sent).isEqualTo(1);
        assertThat(notifier.sent).hasSize(1);

        FakeNotifier.Message message = notifier.sent.getFirst();
        assertThat(message.recipient()).isEqualTo("me@example.com");
        assertThat(message.subject()).isEqualTo("JobRadar: 1 new job for \"Jobs in Israel\"");
        assertThat(message.body()).contains("Backend Engineer").doesNotContain("Data Engineer", "Old Engineer");
    }

    @Test
    void doesNotSendTheSameJobsTwice() {
        saveSearch("Jobs in Israel", null, "israel");
        saveJob("1", "Backend Engineer", "Tel Aviv", "https://example.com/1", 1);

        alertService.sendAlerts();
        int sentSecondTime = alertService.sendAlerts();

        assertThat(sentSecondTime).isZero();
        assertThat(notifier.sent).hasSize(1);
    }

    @Test
    void sendsNothingWhenNoJobMatches() {
        saveSearch("Kotlin jobs", "kotlin", null);
        saveJob("1", "Backend Engineer", "Tel Aviv", "https://example.com/1", 1);

        assertThat(alertService.sendAlerts()).isZero();
        assertThat(notifier.sent).isEmpty();
    }

    @Test
    void ignoresInactiveSearches() {
        SavedSearch search = saveSearch("Jobs in Israel", null, "israel");
        search.deactivate();
        savedSearchRepository.save(search);
        saveJob("1", "Backend Engineer", "Tel Aviv", "https://example.com/1", 1);

        assertThat(alertService.sendAlerts()).isZero();
        assertThat(notifier.sent).isEmpty();
    }

    @Test
    void retriesTheSameJobsAfterAFailedSend() {
        saveSearch("Jobs in Israel", null, "israel");
        saveJob("1", "Backend Engineer", "Tel Aviv", "https://example.com/1", 1);

        notifier.failNextSend = true;
        assertThat(alertService.sendAlerts()).isZero();

        assertThat(alertService.sendAlerts()).isEqualTo(1);
        assertThat(notifier.sent.getFirst().body()).contains("Backend Engineer");
    }

    @Test
    void limitsTheNumberOfJobsInOneEmail() {
        saveSearch("Everything", null, null);
        for (int i = 1; i <= AlertService.MAX_JOBS_PER_EMAIL + 2; i++) {
            saveJob(String.valueOf(i), "Job " + i, "Tel Aviv", "https://example.com/" + i, 1);
        }

        alertService.sendAlerts();

        FakeNotifier.Message message = notifier.sent.getFirst();
        assertThat(message.subject()).contains("12 new jobs");
        assertThat(message.body()).contains("And 2 more.");
    }

    @Test
    void escapesHtmlAndDropsUnsafeLinks() {
        saveSearch("Everything", null, null);
        saveJob("1", "<script>alert('x')</script>", "Tel Aviv", "javascript:alert('x')", 1);

        alertService.sendAlerts();

        String body = notifier.sent.getFirst().body();
        assertThat(body).contains("&lt;script&gt;").doesNotContain("<script>", "javascript:");
    }

    private SavedSearch saveSearch(String name, String query, String country) {
        return savedSearchRepository.save(
                new SavedSearch(name, query, country, null, "me@example.com", searchCreatedAt));
    }

    private void saveJob(String id, String title, String location, String url, int hoursAfterSearch) {
        jobRepository.save(new Job(company, id, title, location, "R&D", url, "Description",
                searchCreatedAt.plus(hoursAfterSearch, ChronoUnit.HOURS)));
    }

    // Keeps the messages instead of sending them, and can simulate a failure.
    private static class FakeNotifier implements Notifier {

        record Message(String recipient, String subject, String body) {
        }

        final List<Message> sent = new ArrayList<>();
        boolean failNextSend = false;

        @Override
        public void send(String recipient, String subject, String htmlBody) {
            if (failNextSend) {
                failNextSend = false;
                throw new IllegalStateException("Simulated failure");
            }
            sent.add(new Message(recipient, subject, htmlBody));
        }
    }
}