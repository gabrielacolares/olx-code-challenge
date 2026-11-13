package br.com.olx.anuncios.integration.moderation.resilience;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class ModerationCooldownTest {
    @Test
    void pausesNewDispatchesForEntireRetryAfterWindowWithoutRealSleep() {
        Instant initial = Instant.parse("2026-01-01T00:00:00Z");
        AtomicReference<Instant> now = new AtomicReference<>(initial);

        List<Duration> sleeps = new ArrayList<>();

        Clock clock = new Clock() {
            @Override
            public ZoneId getZone() {
                return ZoneId.of("UTC");
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return now.get();
            }
        };

        ModerationCooldown cooldown = new ModerationCooldown(clock, duration -> {
            sleeps.add(duration);
            now.updateAndGet(current -> current.plus(duration));
        });

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, "60");

        cooldown.pauseFrom(headers);
        cooldown.awaitAllowed();

        assertEquals(List.of(Duration.ofSeconds(60)), sleeps);
        assertEquals(initial.plusSeconds(60), now.get());
    }

    @Test
    void usesFallbackWhenRetryAfterIsMissing() {
        Instant initial = Instant.parse("2026-01-01T00:00:00Z");

        AtomicReference<Instant> now = new AtomicReference<>(initial);

        List<Duration> sleeps = new ArrayList<>();

        Clock clock = new Clock() {
            @Override
            public ZoneId getZone() {
                return ZoneId.of("UTC");
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return now.get();
            }
        };

        ModerationCooldown cooldown = new ModerationCooldown(clock, duration -> {
            sleeps.add(duration);
            now.updateAndGet(current -> current.plus(duration));
        });

        cooldown.pauseFrom(null);
        cooldown.awaitAllowed();

        assertEquals(List.of(Duration.ofSeconds(1)), sleeps);
    }

    @Test
    void usesFallbackWhenRetryAfterIsInvalid() {
        Instant initial = Instant.parse("2026-01-01T00:00:00Z");
        AtomicReference<Instant> now = new AtomicReference<>(initial);

        List<Duration> sleeps = new ArrayList<>();

        Clock clock = new Clock() {
            @Override
            public ZoneId getZone() {
                return ZoneId.of("UTC");
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return now.get();
            }
        };

        ModerationCooldown cooldown = new ModerationCooldown(clock, duration -> {
            sleeps.add(duration);
            now.updateAndGet(current -> current.plus(duration));
        });

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, "invalid");

        cooldown.pauseFrom(headers);
        cooldown.awaitAllowed();

        assertEquals(List.of(Duration.ofSeconds(1)), sleeps);
    }
}
