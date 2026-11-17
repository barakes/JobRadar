package com.jobradar.alert;

import com.jobradar.model.SavedSearch;
import com.jobradar.repository.JobRepository;
import com.jobradar.repository.JobSummary;
import com.jobradar.repository.SavedSearchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class AlertService {

    static final int MAX_JOBS_PER_EMAIL = 10;

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final SavedSearchRepository savedSearchRepository;
    private final JobRepository jobRepository;
    private final Notifier notifier;

    // Prevents two alert runs from sending the same jobs twice.
    private final AtomicBoolean running = new AtomicBoolean(false);

    public AlertService(SavedSearchRepository savedSearchRepository,
                        JobRepository jobRepository,
                        Notifier notifier) {
        this.savedSearchRepository = savedSearchRepository;
        this.jobRepository = jobRepository;
        this.notifier = notifier;
    }

    // Returns how many emails were sent.
    public int sendAlerts() {
        if (!running.compareAndSet(false, true)) {
            log.info("Alerts are already being sent, skipping this run");
            return 0;
        }

        try {
            int sent = 0;
            for (SavedSearch search : savedSearchRepository.findByActiveTrue()) {
                try {
                    if (sendAlert(search)) {
                        sent++;
                    }
                } catch (RuntimeException e) {
                    // One failing email must not stop the others. This search will be retried next time.
                    log.warn("Alert for saved search {} failed: {}", search.getId(), e.getMessage());
                }
            }
            log.info("Alerts finished: {} emails sent", sent);
            return sent;
        } finally {
            running.set(false);
        }
    }

    private boolean sendAlert(SavedSearch search) {
        // Taken BEFORE the query, so jobs added while we work are caught next time instead of being lost.
        Instant checkedAt = Instant.now();

        Page<JobSummary> matches = jobRepository.findNewMatches(
                search.getQuery(), search.getLocation(), search.getCountry(),
                search.getLastNotifiedAt(), PageRequest.of(0, MAX_JOBS_PER_EMAIL));

        if (matches.isEmpty()) {
            search.markNotified(checkedAt);
            savedSearchRepository.save(search);
            return false;
        }

        notifier.send(search.getEmail(), buildSubject(search, matches), buildBody(search, matches));

        // Only after a successful send. If sending failed, the same jobs will be sent next time.
        search.markNotified(checkedAt);
        savedSearchRepository.save(search);
        return true;
    }

    private static String buildSubject(SavedSearch search, Page<JobSummary> matches) {
        long total = matches.getTotalElements();
        String jobs = total == 1 ? "1 new job" : total + " new jobs";
        return "JobRadar: " + jobs + " for \"" + search.getName() + "\"";
    }

    private static String buildBody(SavedSearch search, Page<JobSummary> matches) {
        StringBuilder html = new StringBuilder();
        html.append("<h2>").append(escape(search.getName())).append("</h2>");
        html.append("<ul>");

        for (JobSummary job : matches.getContent()) {
            html.append("<li>");
            if (isSafeUrl(job.getUrl())) {
                html.append("<a href=\"").append(escape(job.getUrl())).append("\">")
                        .append(escape(job.getTitle())).append("</a>");
            } else {
                html.append(escape(job.getTitle()));
            }
            html.append(" – ").append(escape(job.getCompanyName()));
            if (job.getLocation() != null) {
                html.append(", ").append(escape(job.getLocation()));
            }
            html.append("</li>");
        }

        html.append("</ul>");

        long more = matches.getTotalElements() - matches.getNumberOfElements();
        if (more > 0) {
            html.append("<p>And ").append(more).append(" more.</p>");
        }

        return html.toString();
    }

    // Job data comes from external sources, so it is escaped before being put into HTML.
    private static String escape(String text) {
        return text == null ? "" : HtmlUtils.htmlEscape(text);
    }

    // Blocks links like "javascript:..." that could come from a malicious source.
    private static boolean isSafeUrl(String url) {
        return url != null && (url.startsWith("https://") || url.startsWith("http://"));
    }
}