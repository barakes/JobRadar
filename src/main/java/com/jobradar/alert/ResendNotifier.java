package com.jobradar.alert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

// Sends emails through the Resend HTTP API (plain HTTPS, so it works where SMTP ports are blocked).
@Component
public class ResendNotifier implements Notifier {

    private static final Logger log = LoggerFactory.getLogger(ResendNotifier.class);

    private final RestClient restClient;
    private final String apiKey;
    private final String from;

    public ResendNotifier(@Value("${jobradar.mail.resend-api-key:}") String apiKey,
                          @Value("${jobradar.mail.from:JobRadar <onboarding@resend.dev>}") String from) {
        this.apiKey = apiKey;
        this.from = from;

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(30));

        this.restClient = RestClient.builder()
                .baseUrl("https://api.resend.com")
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public void send(String recipient, String subject, String htmlBody) {
        if (apiKey == null || apiKey.isBlank()) {
            // Without a key (e.g. on a developer machine) emails are only logged, not sent.
            log.info("Email sending is disabled. Would send \"{}\" to {}", subject, recipient);
            return;
        }

        Map<String, Object> body = Map.of(
                "from", from,
                "to", List.of(recipient),
                "subject", subject,
                "html", htmlBody);

        try {
            restClient.post()
                    .uri("/emails")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new IllegalStateException("Email to " + recipient + " could not be sent: " + e.getMessage(), e);
        }
    }
}