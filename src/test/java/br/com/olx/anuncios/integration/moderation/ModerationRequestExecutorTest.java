package br.com.olx.anuncios.integration.moderation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.olx.anuncios.exception.ModerationIntegrationException;
import br.com.olx.anuncios.integration.moderation.dto.ModerationRequest;
import br.com.olx.anuncios.integration.moderation.dto.ModerationResponse;
import br.com.olx.anuncios.integration.moderation.http.ModerationApi;
import br.com.olx.anuncios.model.AdsStatus;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class ModerationRequestExecutorTest {
    @Autowired
    private ModerationClient moderationClient;

    @MockitoBean(name = "moderationApi")
    private ModerationApi moderationApi;

    @BeforeEach
    void preparar() {
        reset(moderationApi);
    }

    @Test
    void sucessoNaoRepeteAChamada() {
        responder(approved());

        assertEquals(AdsStatus.APPROVED, moderacao().getStatus());
        verify(moderationApi, times(1)).check(any(ModerationRequest.class));
    }

    @Test
    void repeteSomenteAResposta429() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, "2");

        HttpClientErrorException tooManyRequests = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", headers, new byte[0], null);

        when(moderationApi.check(any(ModerationRequest.class)))
                .thenThrow(tooManyRequests)
                .thenReturn(approved());

        assertEquals(AdsStatus.APPROVED, moderacao().getStatus());
        verify(moderationApi, times(2)).check(any(ModerationRequest.class));
    }

    @Test
    void esgotaTresTentativasIncluindoATentativaInicial() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, "1");

        HttpClientErrorException tooManyRequests = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", headers, new byte[0], null);

        when(moderationApi.check(any(ModerationRequest.class))).thenThrow(tooManyRequests);

        assertThrows(ModerationIntegrationException.class, this::moderacao);
        verify(moderationApi, times(3)).check(any(ModerationRequest.class));
    }

    @Test
    void erro400NaoGeraRetry() {
        HttpClientErrorException badRequest = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST, "Bad Request", HttpHeaders.EMPTY, new byte[0], null);

        when(moderationApi.check(any(ModerationRequest.class))).thenThrow(badRequest);

        assertThrows(ModerationIntegrationException.class, this::moderacao);
        verify(moderationApi, times(1)).check(any(ModerationRequest.class));
    }

    @Test
    void timeoutDeRespostaNaoGeraRetry() {
        when(moderationApi.check(any(ModerationRequest.class)))
                .thenThrow(new ResourceAccessException("Read timed out", new SocketTimeoutException()));

        assertThrows(ModerationIntegrationException.class, this::moderacao);
        verify(moderationApi, times(1)).check(any(ModerationRequest.class));
    }

    private void responder(ModerationResponse response) {
        when(moderationApi.check(any(ModerationRequest.class))).thenReturn(response);
    }

    private static ModerationResponse approved() {
        ModerationResponse response = new ModerationResponse();
        response.setStatus("APPROVED");

        return response;
    }

    private br.com.olx.anuncios.model.ModerationDecision moderacao() {
        return moderationClient.moderate(
                UUID.randomUUID(), "Bicicleta urbana aro 40", "Em bom estado", BigDecimal.valueOf(100), "esportes");
    }
}
