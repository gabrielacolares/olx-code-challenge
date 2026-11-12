package br.com.olx.anuncios.integration;

import org.springframework.http.HttpHeaders;
import java.time.Duration;

public class RetryAfterDelay {

    private static final Duration FALLBACK = Duration.ofSeconds(1);

    public Duration from(HttpHeaders headers) {

        String value = headers == null ? null : headers.getFirst(HttpHeaders.RETRY_AFTER);

        if (value == null || value.isBlank()) return FALLBACK;

        try {
            long seconds = Long.parseLong(value.trim());
            return seconds < 0 ? FALLBACK : Duration.ofSeconds(seconds);

        } catch (NumberFormatException exception) {
            return FALLBACK;
        }
    }
}
