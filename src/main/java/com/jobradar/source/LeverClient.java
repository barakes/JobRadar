package com.jobradar.source;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.jobradar.model.JobSource;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class LeverClient implements JobSourceClient {

    private static final String BASE_URL = "https://api.lever.co";

    private final RestClient restClient;

    public LeverClient() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(30));

        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public JobSource source() {
        return JobSource.LEVER;
    }

    @Override
    public List<FetchedJob> fetchJobs(String boardToken) {
        LeverPosting[] postings;

        try {
            postings = restClient.get()
                    .uri("/v0/postings/{boardToken}?mode=json", boardToken)
                    .retrieve()
                    .body(LeverPosting[].class);
        } catch (RestClientException e) {
            throw new SourceFetchException("Failed to fetch Lever postings for board " + boardToken, e);
        }

        if (postings == null) {
            throw new SourceFetchException("Lever returned an empty response for board " + boardToken);
        }

        return Arrays.stream(postings)
                .map(LeverClient::toFetchedJob)
                .toList();
    }

    private static FetchedJob toFetchedJob(LeverPosting posting) {
        Categories categories = posting.categories();

        String location = categories == null ? null : categories.location();
        String department = null;

        if (categories != null) {
            department = categories.team() != null ? categories.team() : categories.department();
        }

        return new FetchedJob(
                posting.id(),
                posting.text(),
                location,
                department,
                posting.hostedUrl(),
                buildDescription(posting));
    }

    // Lever splits the description into parts: an intro, lists (requirements, etc.), and a closing section.
    private static String buildDescription(LeverPosting posting) {
        List<String> parts = new ArrayList<>();

        addIfPresent(parts, posting.descriptionPlain());

        if (posting.lists() != null) {
            for (LeverList list : posting.lists()) {
                addIfPresent(parts, list.text());
                if (list.content() != null) {
                    addIfPresent(parts, GreenhouseClient.htmlToText(list.content()));
                }
            }
        }

        addIfPresent(parts, posting.additionalPlain());

        return parts.isEmpty() ? null : String.join("\n\n", parts).strip();
    }

    private static void addIfPresent(List<String> parts, String value) {
        if (value != null && !value.isBlank()) {
            parts.add(value.strip());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record LeverPosting(String id,
                        String text,
                        String hostedUrl,
                        String descriptionPlain,
                        String additionalPlain,
                        Categories categories,
                        List<LeverList> lists) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Categories(String location, String team, String department) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record LeverList(String text, String content) {
    }
}