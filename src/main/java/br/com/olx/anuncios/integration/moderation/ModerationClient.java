package br.com.olx.anuncios.integration.moderation;

import br.com.olx.anuncios.exception.ModerationIntegrationException;
import br.com.olx.anuncios.integration.moderation.dto.ModerationRequest;
import br.com.olx.anuncios.integration.moderation.dto.ModerationResponse;
import br.com.olx.anuncios.model.AdsStatus;
import br.com.olx.anuncios.model.ModerationDecision;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class ModerationClient {
    private static final Logger log = LoggerFactory.getLogger(ModerationClient.class);
    private final ModerationRequestExecutor moderationRequestExecutor;

    public ModerationClient(ModerationRequestExecutor moderationRequestExecutor) {
        this.moderationRequestExecutor = moderationRequestExecutor;
    }

    public ModerationDecision moderate(
            UUID adsId, String title, String description, BigDecimal price, String category) {
        long startedAt = System.nanoTime();
        String previousAdsId = MDC.get("adId");
        MDC.put("adId", adsId.toString());

        try {
            ModerationRequest request = new ModerationRequest(title, description, price, category);
            ModerationResponse response = moderationRequestExecutor.check(request);
            return toDecision(response, adsId, startedAt);
        } catch (ModerationIntegrationException exception) {
            throw exception;
        } catch (Exception exception) {
            logFailure(adsId, exception, startedAt);
            throw new ModerationIntegrationException("Falha ao obter decisao do servico de moderacao", exception);
        } finally {
            restoreMdc(previousAdsId);
        }
    }

    private ModerationDecision toDecision(ModerationResponse response, UUID adsId, long startedAt) {
        if (response == null || response.getStatus() == null) {
            throw invalidResponse(adsId, startedAt, "Resposta vazia ou inválida do serviço de moderação");
        }

        return switch (response.getStatus().toUpperCase(Locale.ROOT)) {
            case "APPROVED" -> new ModerationDecision(AdsStatus.APPROVED, null);
            case "REJECTED" -> new ModerationDecision(AdsStatus.REJECTED, response.getReason());
            default -> throw invalidResponse(adsId, startedAt, "Status desconhecido recebido do serviço de moderação");
        };
    }

    private ModerationIntegrationException invalidResponse(UUID adsId, long startedAt, String message) {
        log.error(
                "operation=moderation adId={} outcome=invalid_response durationMs={}", adsId, elapsedMillis(startedAt));
        return new ModerationIntegrationException(message);
    }

    private void logFailure(UUID adsId, Exception exception, long startedAt) {
        if (exception instanceof RequestNotPermitted) {
            log.error(
                    "operation=moderation adId={} outcome=rate_limiter_timeout durationMs={}",
                    adsId,
                    elapsedMillis(startedAt));
        } else if (exception instanceof RestClientResponseException responseException) {
            int status = responseException.getStatusCode().value();
            if (status == 429) {
                log.error(
                        "operation=moderation adId={} outcome=retries_exhausted status=429 durationMs={}",
                        adsId,
                        elapsedMillis(startedAt));
            } else {
                log.error(
                        "operation=moderation adId={} outcome=upstream_http_failure status={} durationMs={} exceptionType={}",
                        adsId,
                        status,
                        elapsedMillis(startedAt),
                        exception.getClass().getSimpleName());
            }
        } else if (exception instanceof RestClientException) {
            log.error(
                    "operation=moderation adId={} outcome=communication_failure durationMs={} exceptionType={}",
                    adsId,
                    elapsedMillis(startedAt),
                    exception.getClass().getSimpleName());
        } else {
            log.error(
                    "operation=moderation adId={} outcome=unexpected_failure durationMs={} exceptionType={}",
                    adsId,
                    elapsedMillis(startedAt),
                    exception.getClass().getSimpleName());
        }
    }

    private static void restoreMdc(String previousAdsId) {
        if (previousAdsId == null) {
            MDC.remove("adId");
        } else {
            MDC.put("adId", previousAdsId);
        }
    }

    private static long elapsedMillis(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }
}
