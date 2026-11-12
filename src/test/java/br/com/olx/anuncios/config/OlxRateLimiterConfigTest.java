package br.com.olx.anuncios.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OlxRateLimiterConfigTest {
    @Test
    void configuresModerationLimitAndTimeout() {
        RateLimiter rateLimiter = new OlxRateLimiterConfig().moderationRateLimiter();

        assertEquals(1, rateLimiter.getRateLimiterConfig().getLimitForPeriod());
        assertEquals(Duration.ofMillis(251), rateLimiter.getRateLimiterConfig().getLimitRefreshPeriod());
        assertEquals(Duration.ofSeconds(15), rateLimiter.getRateLimiterConfig().getTimeoutDuration());
    }
}
