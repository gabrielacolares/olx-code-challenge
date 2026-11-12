package br.com.olx.anuncios.integration;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Component
public class ModerationCooldown {
    @FunctionalInterface
    interface Sleeper {

        void sleep(Duration duration) throws InterruptedException;

    }

    private final Clock clock;
    private final Sleeper sleeper;
    private final RetryAfterDelay retryAfterDelay;
    private final Object lock = new Object();
    private Instant blockedUntil = Instant.MIN;

    public ModerationCooldown() {
        this(Clock.systemUTC(), duration -> Thread.sleep(duration.toMillis()), new RetryAfterDelay());
    }

    ModerationCooldown(Clock clock, Sleeper sleeper, RetryAfterDelay retryAfterDelay) {
        this.clock = clock;
        this.sleeper = sleeper;
        this.retryAfterDelay = retryAfterDelay;
    }

    public Duration pauseFrom(HttpHeaders headers) {

        Duration retryAfter = retryAfterDelay.from(headers);
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
}