package br.com.olx.anuncios.integration.moderation.http;

import br.com.olx.anuncios.integration.moderation.resilience.ModerationCooldown;
import io.github.resilience4j.ratelimiter.RateLimiter;
import java.io.IOException;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

@Component
public class ModerationRequestInterceptor implements ClientHttpRequestInterceptor {
    private static final Logger log = LoggerFactory.getLogger(ModerationRequestInterceptor.class);

    private final ModerationCooldown moderationCooldown;
    private final RateLimiter moderationRateLimiter;

    public ModerationRequestInterceptor(ModerationCooldown moderationCooldown, RateLimiter moderationRateLimiter) {
        this.moderationCooldown = moderationCooldown;
        this.moderationRateLimiter = moderationRateLimiter;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        awaitPermissionToCall();
        long requestStartedAt = System.nanoTime();
        ClientHttpResponse response = execution.execute(request, body);
        logRateLimitIfNeeded(response, MDC.get("adId"), requestStartedAt);

        return response;
    }

    private void awaitPermissionToCall() {
        moderationCooldown.awaitAllowed();
        RateLimiter.waitForPermission(moderationRateLimiter);

        // Recheck because another request may activate a cooldown while this call waits for a permit.
        moderationCooldown.awaitAllowed();
    }

    private void logRateLimitIfNeeded(ClientHttpResponse response, String adsId, long requestStartedAt)
            throws IOException {
        if (response.getStatusCode().value() != 429) return;

        HttpHeaders headers = response.getHeaders();
        Duration retryAfter = moderationCooldown.pauseFrom(headers);
        String retryAfterHeader = headers.getFirst(HttpHeaders.RETRY_AFTER);

        log.warn(
                "operation=moderation adId={} outcome=rate_limited status=429 retryAfterSeconds={} retryAfterHeaderPresent={} durationMs={}",
                adsId,
                retryAfter.toSeconds(),
                retryAfterHeader != null && !retryAfterHeader.isBlank(),
                elapsedMillis(requestStartedAt));
    }

    private static long elapsedMillis(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }
}
