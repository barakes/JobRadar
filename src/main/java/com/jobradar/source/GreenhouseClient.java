package com.jobradar.source;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jobradar.model.JobSource;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.HtmlUtils;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;

// Reads jobs from the public Greenhouse Job Board API. No authentication is needed for reading.
@Component
public class GreenhouseClient implements JobSourceClient {

    private static final String BASE_URL = "https://boards-api.greenhouse.io";
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");
    private static final Pattern EXTRA_WHITESPACE = Pattern.compile("\\s{2,}");

    private final RestClient restClient;

    public GreenhouseClient() {
        var httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(30));

        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public JobSource source() {
        return JobSource.GREENHOUSE;
    }

    @Override
    public List<FetchedJob> fetchJobs(String boardToken) {
        GreenhouseResponse response;

        try {
            response = restClient.get()
                    .uri("/v1/boards/{boardToken}/jobs?content=true", boardToken)
                    .retrieve()
                    .body(GreenhouseResponse.class);
        } catch (RestClientException ex) {
            throw new SourceFetchException("Could not read Greenhouse board '" + boardToken + "'", ex);
        }

        if (response == null || response.jobs() == null) {
            throw new SourceFetchException("Greenhouse board '" + boardToken + "' returned an empty response");
        }

        return response.jobs().stream()
                .map(GreenhouseClient::toFetchedJob)
                .toList();
    }

    private static FetchedJob toFetchedJob(GreenhouseJob job) {
        String location = job.location() == null ? null : job.location().name();

        String department = job.departments() == null || job.departments().isEmpty()
                ? null
                : job.departments().getFirst().name();

        return new FetchedJob(
                String.valueOf(job.id()),
                job.title(),
                location,
                department,
                job.absoluteUrl(),
                htmlToText(job.content()));
    }

    // The description arrives as HTML that was escaped once more, so it is unescaped, stripped of tags,
    // and unescaped again for entities like &nbsp; that were inside the original HTML.
    static String htmlToText(String escapedHtml) {
        if (escapedHtml == null || escapedHtml.isBlank()) {
            return null;
        }

        String html = HtmlUtils.htmlUnescape(escapedHtml);
        String text = HTML_TAG.matcher(html).replaceAll(" ");
        text = HtmlUtils.htmlUnescape(text);

        return EXTRA_WHITESPACE.matcher(text).replaceAll(" ").trim();
    }

    // Only the fields we use. Any other field in the response is ignored.
    @JsonIgnoreProperties(ignoreUnknown = true)
    record GreenhouseResponse(List<GreenhouseJob> jobs) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GreenhouseJob(
            long id,
            String title,
            NamedItem location,
            @JsonProperty("absolute_url") String absoluteUrl,
            String content,
            List<NamedItem> departments) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record NamedItem(String name) {
    }
}