package com.sonu.jobingestion.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.time.Duration;

@Component
@Slf4j
public class ArbeitnowClient implements JobSourceClient<ArbeitnowResponse> {

    private final WebClient webClient;
    private final String sourceUrl;
    private final int maxRetries;
    private final long backoffDelayMs;

    public ArbeitnowClient(
            WebClient webClient,
            @Value("${job.ingestion.source-url:https://www.arbeitnow.com/api/job-board-api}") String sourceUrl,
            @Value("${job.ingestion.max-retries:3}") int maxRetries,
            @Value("${job.ingestion.backoff-delay-ms:2000}") long backoffDelayMs) {
        this.webClient = webClient;
        this.sourceUrl = sourceUrl;
        this.maxRetries = maxRetries;
        this.backoffDelayMs = backoffDelayMs;
    }

    @Override
    public ArbeitnowResponse fetchJobs(int page) {
        log.info("Requesting raw jobs from Arbeitnow page: {}", page);

        try {
            String uri = org.springframework.web.util.UriComponentsBuilder
                    .fromUriString(sourceUrl)
                    .queryParam("page", page)
                    .build()
                    .toUriString();

            return webClient.get()
                    .uri(uri)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> {
                        log.error("Client error (4xx) received from source. Status code: {}", clientResponse.statusCode());
                        return clientResponse.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new WebClientResponseException(
                                        clientResponse.statusCode().value(),
                                        "Client Error: " + body,
                                        null, null, null)));
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, serverResponse -> {
                        log.error("Server error (5xx) received from source. Status code: {}", serverResponse.statusCode());
                        return serverResponse.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new WebClientResponseException(
                                        serverResponse.statusCode().value(),
                                        "Server Error: " + body,
                                        null, null, null)));
                    })
                    .bodyToMono(ArbeitnowResponse.class)
                    .retryWhen(Retry.backoff(maxRetries, Duration.ofMillis(backoffDelayMs))
                            .filter(this::isTransientError)
                            .doBeforeRetry(retrySignal -> log.warn("Retrying API request due to transient failure. Attempt: {} of {}", 
                                    retrySignal.totalRetries() + 1, maxRetries))
                    )
                    .block(); // Blocking here as the ingestion pipeline runs synchronously on schedule or REST triggers

        } catch (WebClientResponseException e) {
            log.error("HTTP Exception during job fetching: Status = {}, Response = {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected exception during job fetching: {}", e.getMessage(), e);
            throw e;
        }
    }

    private boolean isTransientError(Throwable throwable) {
        if (throwable instanceof IOException) {
            return true; // network timeouts, socket close, DNS failure
        }
        if (throwable instanceof WebClientResponseException webEx) {
            int status = webEx.getStatusCode().value();
            return status >= 500 || status == 408 || status == 429; // Server errors, timeout, or rate-limited
        }
        String msg = throwable.getMessage();
        return msg != null && (msg.contains("Timeout") || msg.contains("timeout") || msg.contains("Connection") || msg.contains("connection"));
    }
}
