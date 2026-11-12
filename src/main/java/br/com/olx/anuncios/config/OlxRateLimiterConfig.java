package br.com.olx.anuncios.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class OlxRateLimiterConfig {
    @Bean
    public RateLimiter moderationRateLimiter() {

        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(1)
                .limitRefreshPeriod(Duration.ofMillis(251))
                .timeoutDuration(Duration.ofSeconds(15))
                .build();

        RateLimiterRegistry registry = RateLimiterRegistry.of(config);

        return registry.rateLimiter("olxModerationApi");
    }
}
