package br.com.olx.anuncios.controller;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.olx.anuncios.dto.AdsRequest;
import br.com.olx.anuncios.exception.ModerationIntegrationException;
import br.com.olx.anuncios.integration.moderation.ModerationClient;
import br.com.olx.anuncios.model.AdsStatus;
import br.com.olx.anuncios.model.ModerationDecision;
import br.com.olx.anuncios.repository.AdsRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AdsControllerTest {
    private static final String TITLE = "Urban bicycle, 29-inch wheels";
    private static final String DESCRIPTION = "Bicycle in great condition";
    private static final String CATEGORY = "esportes";
    private static final String IMAGE_URL = "https://img.test/bicicleta.jpg";

    @LocalServerPort
    private int port;

    @Autowired
    private AdsRepository adsRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ModerationClient moderationClient;

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://[::1]";
        RestAssured.port = port;
        adsRepository.deleteAll();

        when(moderationClient.moderate(any(UUID.class), anyString(), anyString(), any(), anyString()))
                .thenReturn(new ModerationDecision(AdsStatus.APPROVED, null));
    }

    @AfterEach
    void resetRestAssuredConfiguration() {
        RestAssured.reset();
    }

    @Test
    void createsApprovedAdAndReturns201() {
        post(validRequest())
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("title", equalTo(TITLE))
                .body("description", equalTo(DESCRIPTION))
                .body("price", equalTo(850.0f))
                .body("category", equalTo(CATEGORY))
                .body("images", equalTo(List.of(IMAGE_URL)))
                .body("status", equalTo("APPROVED"))
                .body("rejectionReason", org.hamcrest.Matchers.nullValue());

        org.junit.jupiter.api.Assertions.assertEquals(1, adsRepository.count());
    }

    @ParameterizedTest
    @ValueSource(strings = {"eletronicos", "moveis", "veiculos", "imoveis", "esportes", "colecionaveis"})
    void acceptsEachCategoryDefinedByModerationApi(String category) {
        AdsRequest request = validRequest();
        request.setCategory(category);

        post(request).then().statusCode(201).body("category", equalTo(category));
    }

    @ParameterizedTest
    @ValueSource(strings = {"contact_info_not_allowed", "prohibited_item", "price_out_of_range"})
    void returnsAndPersistsEachModerationRejectionReason(String reason) {
        when(moderationClient.moderate(any(UUID.class), anyString(), anyString(), any(), anyString()))
                .thenReturn(new ModerationDecision(AdsStatus.REJECTED, reason));

        post(validRequest())
                .then()
                .statusCode(201)
                .body("status", equalTo("REJECTED"))
                .body("rejectionReason", equalTo(reason));

        org.junit.jupiter.api.Assertions.assertEquals(1, adsRepository.count());
    }

    @ParameterizedTest
    @CsvSource({
        "eletronicos, 9.99",
        "eletronicos, 50000.01",
        "moveis, 19.99",
        "moveis, 30000.01",
        "veiculos, 999.99",
        "veiculos, 500000.01",
        "imoveis, 9999.99",
        "imoveis, 5000000.01",
        "esportes, 4.99",
        "esportes, 20000.01",
        "colecionaveis, 0.99",
        "colecionaveis, 100000.01"
    })
    void persistsPriceOutOfRangeRejectionReturnedByModeration(String category, String price) {
        BigDecimal amount = new BigDecimal(price);
        AdsRequest request = validRequest();
        request.setCategory(category);
        request.setPrice(amount);
        when(moderationClient.moderate(any(UUID.class), anyString(), anyString(), eq(amount), eq(category)))
                .thenReturn(new ModerationDecision(AdsStatus.REJECTED, "price_out_of_range"));

        post(request)
                .then()
                .statusCode(201)
                .body("category", equalTo(category))
                .body("price", equalTo(amount.floatValue()))
                .body("status", equalTo("REJECTED"))
                .body("rejectionReason", equalTo("price_out_of_range"));

        org.junit.jupiter.api.Assertions.assertEquals(1, adsRepository.count());
        org.mockito.Mockito.verify(moderationClient)
                .moderate(any(UUID.class), eq(TITLE), eq(DESCRIPTION), eq(amount), eq(category));
    }

    @ParameterizedTest
    @ValueSource(strings = {"categoria_inexistente", "ESPORTES"})
    void returns400ForUnsupportedCategoryWithoutCallingModeration(String category) {
        AdsRequest request = validRequest();
        request.setCategory(category);
        doThrow(new ModerationIntegrationException("Falha ao obter decisao do servico de moderacao"))
                .when(moderationClient)
                .moderate(any(UUID.class), anyString(), anyString(), any(), anyString());

        Response response = post(request);
        org.junit.jupiter.api.Assertions.assertEquals(400, response.statusCode());
        org.junit.jupiter.api.Assertions.assertEquals("validation_error", response.path("error"));
        assertThat((List<?>) response.path("details")).hasSizeGreaterThan(0);
        verifyNoInteractions(moderationClient);
    }

    @Test
    void returns400WhenRequiredFieldsAreNull() {
        AdsRequest request = new AdsRequest();
        request.setTitle(null);
        request.setDescription(null);
        request.setPrice(null);
        request.setCategory(null);
        request.setImages(null);

        Response response = post(request);
        org.junit.jupiter.api.Assertions.assertEquals(400, response.statusCode());
        org.junit.jupiter.api.Assertions.assertEquals("validation_error", response.path("error"));
        assertThat((List<?>) response.path("details")).hasSizeGreaterThan(1);
        verifyNoInteractions(moderationClient);
    }

    @Test
    void returnsAllValidationErrorsWhenMultipleFieldsAreInvalid() {
        AdsRequest request = validRequest();
        request.setTitle("Short");
        request.setDescription(" ");
        request.setPrice(BigDecimal.ZERO);
        request.setCategory("categoria_inexistente");
        request.setImages(List.of("ftp://img.test/ad.jpg"));

        Response response = post(request);
        org.junit.jupiter.api.Assertions.assertEquals(400, response.statusCode());
        org.junit.jupiter.api.Assertions.assertEquals("validation_error", response.path("error"));
        assertThat((List<?>) response.path("details")).hasSizeGreaterThan(1);
        verifyNoInteractions(moderationClient);
    }

    @Test
    void returns400WhenARequiredFieldIsOmitted() {
        Response response = given().contentType(ContentType.JSON)
                .body("{\"title\":\"Urban bicycle, 29-inch wheels\","
                        + "\"description\":\"Bicycle in great condition\","
                        + "\"price\":850,"
                        + "\"images\":[\"https://img.test/bicicleta.jpg\"]}")
                .when()
                .post("/api/ads");

        org.junit.jupiter.api.Assertions.assertEquals(400, response.statusCode());
        org.junit.jupiter.api.Assertions.assertEquals("validation_error", response.path("error"));
        assertThat((List<?>) response.path("details")).hasSizeGreaterThan(0);
        verifyNoInteractions(moderationClient);
    }

    @Test
    void returns400WhenTitleIsTooShort() {
        AdsRequest request = validRequest();
        request.setTitle("Short");

        assertBadRequest(request);
        verifyNoInteractions(moderationClient);
    }

    @Test
    void returns400WhenTitleIsTooLong() {
        AdsRequest request = validRequest();
        request.setTitle("A".repeat(101));

        assertBadRequest(request);
        verifyNoInteractions(moderationClient);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-0.01"})
    void returns400WhenPriceIsNotPositive(String price) {
        AdsRequest request = validRequest();
        request.setPrice(new BigDecimal(price));

        assertBadRequest(request);
        verifyNoInteractions(moderationClient);
    }

    @Test
    void returns400WhenPriceHasTheWrongJsonType() {
        Response response = given().contentType(ContentType.JSON)
                .body("{\"title\":\"Urban bicycle, 29-inch wheels\","
                        + "\"description\":\"Bicycle in great condition\","
                        + "\"price\":\"not-a-number\","
                        + "\"category\":\"esportes\","
                        + "\"images\":[\"https://img.test/bicicleta.jpg\"]}")
                .when()
                .post("/api/ads");

        org.junit.jupiter.api.Assertions.assertEquals(400, response.statusCode());
        org.junit.jupiter.api.Assertions.assertEquals("validation_error", response.path("error"));
        assertThat((List<?>) response.path("details")).hasSizeGreaterThan(0);
        verifyNoInteractions(moderationClient);
    }

    @Test
    void returns400WhenImagesAreEmpty() {
        AdsRequest request = validRequest();
        request.setImages(List.of());

        assertBadRequest(request);
        verifyNoInteractions(moderationClient);
    }

    @Test
    void returns400WhenImageUrlHasAnUnsupportedScheme() {
        AdsRequest request = validRequest();
        request.setImages(List.of("ftp://img.test/ad.jpg"));

        assertBadRequest(request);
        verifyNoInteractions(moderationClient);
    }

    @Test
    void returnsInvalidJsonErrorForMalformedJson() {
        Response response =
                given().contentType(ContentType.JSON).body("{\"title\":").when().post("/api/ads");

        org.junit.jupiter.api.Assertions.assertEquals(400, response.statusCode());
        org.junit.jupiter.api.Assertions.assertEquals("invalid_json", response.path("error"));
        verifyNoInteractions(moderationClient);
    }

    @Test
    void returns502WithoutBodyWhenModerationFails() {
        doThrow(new ModerationIntegrationException("Falha no serviço externo"))
                .when(moderationClient)
                .moderate(any(UUID.class), anyString(), anyString(), any(), anyString());

        Response response = post(validRequest());

        org.junit.jupiter.api.Assertions.assertEquals(502, response.statusCode());
        assertThat(response.asString()).isEmpty();
    }

    private void assertBadRequest(AdsRequest request) {
        post(request).then().statusCode(400);
    }

    private Response post(AdsRequest request) {
        return given().contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .when()
                .post("/api/ads");
    }

    private AdsRequest validRequest() {
        AdsRequest request = new AdsRequest();
        request.setTitle(TITLE);
        request.setDescription(DESCRIPTION);
        request.setPrice(new BigDecimal("850.00"));
        request.setCategory(CATEGORY);
        request.setImages(List.of(IMAGE_URL));
        return request;
    }
}
