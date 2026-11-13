package br.com.olx.anuncios.integration.moderation.resilience;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
public class ModerationCooldown {
    private static final Duration FALLBACK = Duration.ofSeconds(1);

    @FunctionalInterface
    interface Sleeper {
        void sleep(Duration duration) throws InterruptedException;
    }

    private final Clock clock;
    private final Sleeper sleeper;
    private final Object lock = new Object();
    private Instant blockedUntil = Instant.MIN;

    public ModerationCooldown() {
        this(Clock.systemUTC(), duration -> Thread.sleep(duration.toMillis()));
    }

    ModerationCooldown(Clock clock, Sleeper sleeper) {
        this.clock = clock;
        this.sleeper = sleeper;
    }

    public Duration pauseFrom(HttpHeaders headers) {
        Duration retryAfter = parseRetryAfter(headers);
        Instant nextAllowed = clock.instant().plus(retryAfter);

        synchronized (lock) {
            if (nextAllowed.isAfter(blockedUntil)) {
                blockedUntil = nextAllowed;
            }
        }

        return retryAfter;
    }

    public void awaitAllowed() {
        while (true) {
            Duration remaining;
            synchronized (lock) {
                remaining = Duration.between(clock.instant(), blockedUntil);
            }

            if (remaining.isZero() || remaining.isNegative()) return;

            try {
                sleeper.sleep(remaining);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrompido durante pausa da moderacao", exception);
            }
        }
    }

    private Duration parseRetryAfter(HttpHeaders headers) {
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
