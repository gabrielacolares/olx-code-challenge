package br.com.olx.anuncios.integration;

import br.com.olx.anuncios.exception.ModerationIntegrationException;
import br.com.olx.anuncios.integration.dto.ModerationRequest;
import br.com.olx.anuncios.integration.dto.ModerationResponse;
import br.com.olx.anuncios.model.AdsStatus;
import br.com.olx.anuncios.model.ModerationDecision;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ModerationClient {
    private static final Logger log = LoggerFactory.getLogger(ModerationClient.class);
    private static final int MAX_ATTEMPTS = 3;

    private final RestClient restClient;
    private final Retry moderationRetry;
    private final ModerationCooldown moderationCooldown;
    private final RateLimiter moderationRateLimiter;

    public ModerationClient(@Value("${moderation.base-url:http://localhost:8787}") String baseUrl,
                            ModerationCooldown moderationCooldown,
                            RateLimiter moderationRateLimiter) {

        this.moderationCooldown = moderationCooldown;

        this.moderationRateLimiter = moderationRateLimiter;

        this.restClient = RestClient.builder().baseUrl(baseUrl).build();

        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(MAX_ATTEMPTS)
                .retryOnException(ModerationClient::isRateLimited)
                .intervalBiFunction((attempt, outcome) -> 0L)
                .build();

        this.moderationRetry = Retry.of("moderation-429", retryConfig);
    }

    private static boolean isRateLimited(Throwable throwable) {

        return throwable instanceof RestClientResponseException responseException
                && responseException.getStatusCode().value() == 429;
    }

    private ModerationResponse requestModeration(ModerationRequest request, UUID adsId, int attempt) {
        moderationCooldown.awaitAllowed();

        RateLimiter.waitForPermission(moderationRateLimiter);

        moderationCooldown.awaitAllowed();

        long startedAt = System.nanoTime();

        try {
            return restClient.post()
                    .uri("/v1/moderation/check")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ModerationResponse.class);
        } catch (RestClientResponseException exception) {

            long durationMs = elapsedMillis(startedAt);
            int status = exception.getStatusCode().value();

            if (status == 429) {

                HttpHeaders headers = exception.getResponseHeaders();

                Duration retryAfter = moderationCooldown.pauseFrom(headers);

                String retryAfterValue = headers == null ? null : headers.getFirst(HttpHeaders.RETRY_AFTER);

                boolean retryAfterHeaderPresent = retryAfterValue != null && !retryAfterValue.isBlank();

                log.warn("operation=moderation adId={} outcome=rate_limited status={} attempt={} retryAfterSeconds={} retryAfterHeaderPresent={} durationMs={}",
                        adsId, status, attempt, retryAfter.toMillis() / 1000.0, retryAfterHeaderPresent, durationMs);
            } else {
                String errorType = status == 400 ? "payload_error"
                        : status >= 500 ? "temporary_server_error" : "client_error";
                log.error("operation=moderation adId={} outcome=http_error errorType={} status={} attempt={} durationMs={}",
                        adsId, errorType, status, attempt, durationMs);
            }

            throw exception;

        } catch (RestClientException exception) {

            log.error("operation=moderation adId={} outcome=communication_failure attempt={} durationMs={} exceptionType={}",
                    adsId, attempt, elapsedMillis(startedAt), exception.getClass().getSimpleName());
            throw exception;
        }
    }

    public ModerationDecision moderate(UUID adsId, String title, String description, BigDecimal price, String category) {
        long startedAt = System.nanoTime();

        AtomicInteger attempts = new AtomicInteger();

        try {
            ModerationRequest request = new ModerationRequest(title, description, price, category);

            ModerationResponse response = moderationRetry.executeCallable(
                    () -> requestModeration(request, adsId, attempts.incrementAndGet()));


            if (response == null || response.getStatus() == null) {
                log.error("operation=moderation adId={} outcome=invalid_response durationMs={}",
                        adsId, elapsedMillis(startedAt));

                throw new ModerationIntegrationException("Resposta vazia ou inválida do serviço de moderação");
            }

            ModerationDecision decision = switch (response.getStatus().toUpperCase(Locale.ROOT)) {
                case "APPROVED" -> new ModerationDecision(AdsStatus.APPROVED, null);

                case "REJECTED" -> new ModerationDecision(AdsStatus.REJECTED, response.getReason());

                default -> {
                    log.error("operation=moderation adId={} outcome=invalid_response durationMs={}",
                            adsId, elapsedMillis(startedAt));
                    throw new ModerationIntegrationException(
                            "Status desconhecido recebido do serviço de moderação");
                }
            };

            if (decision.getStatus() == AdsStatus.REJECTED) {
                log.warn("operation=moderation adId={} outcome=rejected reason={} attempts={} durationMs={}",
                        adsId, decision.getRejectionReason(), attempts.get(), elapsedMillis(startedAt));

            } else {
                log.info("operation=moderation adId={} outcome=approved attempts={} durationMs={}",
                        adsId, attempts.get(), elapsedMillis(startedAt));
            }

            return decision;
        } catch (Exception exception) {

            if (exception instanceof ModerationIntegrationException integrationException) {
                throw integrationException;
            }

            if (exception instanceof RequestNotPermitted) {
                log.error("operation=moderation adId={} outcome=rate_limiter_timeout attempts={} durationMs={}",
                        adsId, attempts.get(), elapsedMillis(startedAt));

            } else if (isRateLimited(exception)) {
                log.error("operation=moderation adId={} outcome=retries_exhausted status=429 attempts={} durationMs={}",
                        adsId, attempts.get(), elapsedMillis(startedAt));
            } else if (!(exception instanceof RestClientException)) {
                log.error("operation=moderation adId={} outcome=unexpected_failure attempts={} durationMs={} exceptionType={}",
                        adsId, attempts.get(), elapsedMillis(startedAt), exception.getClass().getSimpleName());
            }
            throw new ModerationIntegrationException(
                    "Falha ao obter decisao do servico de moderacao", exception);
        }
    }

    private static long elapsedMillis(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }
}