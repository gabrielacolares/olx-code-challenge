package br.com.olx.anuncios.config;

import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.event.RetryOnRetryEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClientResponseException;

@Configuration
public class ModerationRetryLoggingConfiguration {
    private static final Logger log = LoggerFactory.getLogger(ModerationRetryLoggingConfiguration.class);

    @Bean
    SmartInitializingSingleton moderationRetryEventLogger(RetryRegistry retryRegistry) {
        return () -> retryRegistry.retry("moderation").getEventPublisher().onRetry(this::logRetry);
    }

    private void logRetry(RetryOnRetryEvent event) {
        Throwable failure = event.getLastThrowable();
        Integer status = failure instanceof RestClientResponseException responseException
                ? responseException.getStatusCode().value()
                : null;
        log.warn(
                "operation=moderation adId={} outcome=retry status={} attempt={} retryName={}",
                MDC.get("adId"),
                status,
                event.getNumberOfRetryAttempts(),
                event.getName());
    }
}
