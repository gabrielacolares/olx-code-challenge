package br.com.olx.anuncios.integration.moderation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class ModerationRequestExecutorTest {
    private static final String TITLE = "Bicicleta urbana aro 40";
    private static final String DESCRIPTION = "Em bom estado";
    private static final BigDecimal PRICE = BigDecimal.valueOf(100);
    private static final String CATEGORY = "esportes";

    @Autowired
    private ModerationClient moderationClient;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean(name = "moderationApi")
    private ModerationApi moderationApi;

    @BeforeEach
    void setUp() {
        reset(moderationApi);
    }

    @Test
    void successfulResponseSendsAllContractFieldsWithoutRetry() {
        mockModerationResponse(approved());

        assertEquals(AdsStatus.APPROVED, moderateSampleAd().getStatus());

        ArgumentCaptor<ModerationRequest> requestCaptor = ArgumentCaptor.forClass(ModerationRequest.class);
        verify(moderationApi, times(1)).check(requestCaptor.capture());

        ModerationRequest request = requestCaptor.getValue();
        assertEquals(TITLE, request.getTitle());
        assertEquals(DESCRIPTION, request.getDescription());
        assertEquals(PRICE, request.getPrice());
        assertEquals(CATEGORY, request.getCategory());
    }

    @ParameterizedTest
    @ValueSource(strings = {"contact_info_not_allowed", "prohibited_item", "price_out_of_range"})
    void mapsAllDocumentedRejectionReasons(String reason) {
        ModerationResponse response = new ModerationResponse();
        response.setStatus("rejected");
        response.setReason(reason);
        mockModerationResponse(response);

        var decision = moderateSampleAd();

        assertEquals(AdsStatus.REJECTED, decision.getStatus());
        assertEquals(reason, decision.getRejectionReason());
        verify(moderationApi, times(1)).check(any(ModerationRequest.class));
    }

    @Test
    void mapsRejectionReasonFromMockSnakeCaseField() throws Exception {
        ModerationResponse response = objectMapper.readValue(
                "{\"status\":\"rejected\",\"rejection_reason\":\"price_out_of_range\"}",
                ModerationResponse.class);
        mockModerationResponse(response);

        var decision = moderateSampleAd();

        assertEquals(AdsStatus.REJECTED, decision.getStatus());
        assertEquals("price_out_of_range", decision.getRejectionReason());
    }

    @Test
    void treatsRejectedResponseWithoutReasonAsIntegrationFailure() {
        ModerationResponse response = new ModerationResponse();
        response.setStatus("rejected");
        mockModerationResponse(response);

        assertThrows(ModerationIntegrationException.class, this::moderateSampleAd);
        verify(moderationApi, times(1)).check(any(ModerationRequest.class));
    }

    @Test
    void retriesOnlyTooManyRequestsResponses() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, "2");

        HttpClientErrorException tooManyRequests = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", headers, new byte[0], null);

        when(moderationApi.check(any(ModerationRequest.class)))
                .thenThrow(tooManyRequests)
                .thenReturn(approved());

        assertEquals(AdsStatus.APPROVED, moderateSampleAd().getStatus());
        verify(moderationApi, times(2)).check(any(ModerationRequest.class));
    }

    @Test
    void stopsAfterThreeTotalAttemptsIncludingInitialRequest() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, "1");

        HttpClientErrorException tooManyRequests = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", headers, new byte[0], null);

        when(moderationApi.check(any(ModerationRequest.class))).thenThrow(tooManyRequests);

        assertThrows(ModerationIntegrationException.class, this::moderateSampleAd);
        verify(moderationApi, times(3)).check(any(ModerationRequest.class));
    }

    @Test
    void doesNotRetryBadRequestResponses() {
        HttpClientErrorException badRequest = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST, "Bad Request", HttpHeaders.EMPTY, new byte[0], null);

        when(moderationApi.check(any(ModerationRequest.class))).thenThrow(badRequest);

        assertThrows(ModerationIntegrationException.class, this::moderateSampleAd);
        verify(moderationApi, times(1)).check(any(ModerationRequest.class));
    }

    @ParameterizedTest
    @MethodSource("badRequestPayloads")
    void preservesMockBadRequestPayloadWithoutRetry(String errorPayload) {
        HttpClientErrorException badRequest = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                HttpHeaders.EMPTY,
                errorPayload.getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);

        when(moderationApi.check(any(ModerationRequest.class))).thenThrow(badRequest);

        ModerationIntegrationException failure = assertThrows(ModerationIntegrationException.class, this::moderateSampleAd);
        HttpClientErrorException cause = assertInstanceOf(HttpClientErrorException.class, failure.getCause());

        assertEquals(errorPayload, cause.getResponseBodyAsString());
        verify(moderationApi, times(1)).check(any(ModerationRequest.class));
    }

    @Test
    void doesNotRetryInternalServerErrorResponses() {
        HttpServerErrorException internalServerError = HttpServerErrorException.create(
                HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", HttpHeaders.EMPTY, new byte[0], null);

        when(moderationApi.check(any(ModerationRequest.class))).thenThrow(internalServerError);

        assertThrows(ModerationIntegrationException.class, this::moderateSampleAd);
        verify(moderationApi, times(1)).check(any(ModerationRequest.class));
    }

    @Test
    void doesNotRetryResponseTimeouts() {
        when(moderationApi.check(any(ModerationRequest.class)))
                .thenThrow(new ResourceAccessException("Read timed out", new SocketTimeoutException()));

        assertThrows(ModerationIntegrationException.class, this::moderateSampleAd);
        verify(moderationApi, times(1)).check(any(ModerationRequest.class));
    }

    private static Stream<Arguments> badRequestPayloads() {
        return Stream.of(
                Arguments.of("{\"error\":\"validation_error\",\"details\":[\"title is required\"]}"),
                Arguments.of("{\"error\":\"invalid_json\"}"));
    }

    private void mockModerationResponse(ModerationResponse response) {
        when(moderationApi.check(any(ModerationRequest.class))).thenReturn(response);
    }

    private static ModerationResponse approved() {
        ModerationResponse response = new ModerationResponse();
        response.setStatus("approved");

        return response;
    }

    private br.com.olx.anuncios.model.ModerationDecision moderateSampleAd() {
        return moderationClient.moderate(UUID.randomUUID(), TITLE, DESCRIPTION, PRICE, CATEGORY);
    }
}
