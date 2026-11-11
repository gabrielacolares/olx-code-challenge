package br.com.olx.anuncios.integration;

import br.com.olx.anuncios.exception.ModerationIntegrationException;
import br.com.olx.anuncios.model.AdsStatus;
import br.com.olx.anuncios.model.ModerationDecision;
import br.com.olx.anuncios.integration.dto.ModerationRequest;
import br.com.olx.anuncios.integration.dto.ModerationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.Locale;

@Component
public class ModerationClient {

    private final RestClient restClient;

    public ModerationClient(@Value("${moderation.base-url:http://localhost:8787}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public ModerationDecision moderate(String title, String description, BigDecimal price, String category) {
        try {
            ModerationRequest request = new ModerationRequest(title, description, price, category);

            ModerationResponse response = restClient.post()
                    .uri("/v1/moderation/check")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ModerationResponse.class);

            if (response == null || response.getStatus() == null) {
                throw new ModerationIntegrationException("Resposta vazia ou inválida do serviço de moderação");
            }

            return switch (response.getStatus().toUpperCase(Locale.ROOT)) {
                case "APPROVED" -> new ModerationDecision(AdsStatus.APPROVED, null);
                case "REJECTED" -> new ModerationDecision(AdsStatus.REJECTED, response.getReason());
                default -> throw new ModerationIntegrationException(
                        "Status desconhecido recebido do serviço de moderação");
            };

        } catch (RestClientException | IllegalArgumentException exception) {
            throw new ModerationIntegrationException(
                    "Não foi possível obter uma decisão válida do serviço de moderação", exception);
        }
    }
}
