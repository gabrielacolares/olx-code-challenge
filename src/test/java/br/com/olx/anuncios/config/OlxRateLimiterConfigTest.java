package br.com.olx.anuncios.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.resilience4j.ratelimiter.RateLimiter;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class OlxRateLimiterConfigTest {
    @Test
    void configuresModerationLimitAndTimeout() {
        RateLimiter rateLimiter = new RateLimiterConfiguration().moderationRateLimiter();

        assertEquals(1, rateLimiter.getRateLimiterConfig().getLimitForPeriod());
        assertEquals(Duration.ofMillis(251), rateLimiter.getRateLimiterConfig().getLimitRefreshPeriod());
        assertEquals(Duration.ofSeconds(15), rateLimiter.getRateLimiterConfig().getTimeoutDuration());
    }
}
