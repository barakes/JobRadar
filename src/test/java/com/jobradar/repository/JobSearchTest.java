package com.jobradar.repository;

import com.jobradar.IntegrationTest;
import com.jobradar.model.Company;
import com.jobradar.model.Job;
import com.jobradar.model.JobSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class JobSearchTest extends IntegrationTest {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private JobRepository jobRepository;

    private Company acme;
    private final Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);

    @BeforeEach
    void setUp() {
        jobRepository.deleteAll();
        companyRepository.deleteAll();

        acme = companyRepository.save(new Company("Acme", JobSource.GREENHOUSE, "acme"));
        Company globex = companyRepository.save(new Company("Globex", JobSource.LEVER, "globex"));

        // Each job is "first seen" one hour after the previous one, so the order is predictable.
        save(acme, "1", "Backend Engineer", "Tel Aviv", "We use Java and PostgreSQL.", 1);
        save(acme, "2", "Senior Backend Engineer", "Israel", "Kotlin and Kafka.", 2);
        save(globex, "3", "Frontend Engineer", "London, UK", "React and TypeScript.", 3);
        save(globex, "4", "Product Manager", "Petah Tikva", "Own the roadmap.", 4);
    }

    @Test
    void withoutFiltersReturnsAllOpenJobsNewestFirst() {
        Page<JobSummary> page = search(null, null, null);

        assertThat(page.getContent())
                .extracting(JobSummary::getTitle)
                .containsExactly("Product Manager", "Frontend Engineer", "Senior Backend Engineer", "Backend Engineer");
    }

    @Test
    void returnsCompanyNameAndCountry() {
        JobSummary job = search("frontend", null, null).getContent().getFirst();

        assertThat(job.getCompanyName()).isEqualTo("Globex");
        assertThat(job.getCountry()).isEqualTo("United Kingdom");
    }

    @Test
    void searchesInTitleAndDescription() {
        assertThat(titles(search("backend", null, null)))
                .containsExactlyInAnyOrder("Backend Engineer", "Senior Backend Engineer");

        // "kafka" appears only in the description.
        assertThat(titles(search("kafka", null, null)))
                .containsExactly("Senior Backend Engineer");
    }

    @Test
    void supportsExcludingWords() {
        assertThat(titles(search("backend -senior", null, null)))
                .containsExactly("Backend Engineer");
    }

    @Test
    void filtersByCountryIgnoringCase() {
        assertThat(titles(search(null, null, "israel")))
                .containsExactlyInAnyOrder("Backend Engineer", "Senior Backend Engineer", "Product Manager");
    }

    @Test
    void filtersByPartOfTheLocation() {
        assertThat(titles(search(null, "tel", null)))
                .containsExactly("Backend Engineer");
    }

    @Test
    void combinesTextAndCountry() {
        assertThat(titles(search("engineer", null, "israel")))
                .containsExactlyInAnyOrder("Backend Engineer", "Senior Backend Engineer");
    }

    @Test
    void excludesClosedJobs() {
        Job closed = jobRepository.findByCompany(acme).stream()
                .filter(job -> job.getExternalId().equals("1"))
                .findFirst()
                .orElseThrow();
        closed.markClosed();
        jobRepository.save(closed);

        assertThat(titles(search("backend", null, null)))
                .containsExactly("Senior Backend Engineer");
    }

    @Test
    void splitsResultsIntoPages() {
        Page<JobSummary> firstPage = jobRepository.search(null, null, null, PageRequest.of(0, 3));
        Page<JobSummary> secondPage = jobRepository.search(null, null, null, PageRequest.of(1, 3));

        assertThat(firstPage.getContent()).hasSize(3);
        assertThat(secondPage.getContent()).hasSize(1);
        assertThat(firstPage.getTotalElements()).isEqualTo(4);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
    }

    @Test
    void returnsNothingWhenNothingMatches() {
        Page<JobSummary> page = search("cobol", null, null);

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }

    private Page<JobSummary> search(String query, String location, String country) {
        return jobRepository.search(query, location, country, PageRequest.of(0, 20));
    }

    private static java.util.List<String> titles(Page<JobSummary> page) {
        return page.getContent().stream().map(JobSummary::getTitle).toList();
    }

    private void save(Company company, String id, String title, String location, String description, int hoursLater) {
        jobRepository.save(new Job(company, id, title, location, "R&D",
                "https://example.com/jobs/" + id, description, now.plus(hoursLater, ChronoUnit.HOURS)));
    }
}