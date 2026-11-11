package br.com.olx.anuncios.controller;

import br.com.olx.anuncios.dto.AdsRequest;
import br.com.olx.anuncios.integration.ModerationClient;
import br.com.olx.anuncios.model.AdsStatus;
import br.com.olx.anuncios.model.ModerationDecision;
import br.com.olx.anuncios.repository.AdsRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AdsControllerTest {
    private static final String TITLE = "Bicicleta urbana aro 29";
    private static final String DESCRIPTION = "Bicicleta em ótimo estado";
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
    void prepararTeste() {
        RestAssured.baseURI = "http://[::1]";
        RestAssured.port = port;
        adsRepository.deleteAll();
        when(moderationClient.moderate(anyString(), anyString(), any(), anyString()))
                .thenReturn(new ModerationDecision(AdsStatus.APPROVED, null));
    }

    @AfterEach
    void limparConfiguracaoRestAssured() {
        RestAssured.reset();
    }

    @Test
    void deveCriarAnuncioValidoERetornar201() {
        // Arrange
        AdsRequest request = criarRequisicaoValida();

        // Act
        var resposta = given()
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
        .when()
                .post("/api/ads");

        // Assert
        resposta.then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("title", equalTo(TITLE))
                .body("description", equalTo(DESCRIPTION))
                .body("price", equalTo(850.0f))
                .body("category", equalTo(CATEGORY))
                .body("images", equalTo(List.of(IMAGE_URL)));
    }

    @Test
    void deveRetornar400QuandoOsCamposForemInvalidosOuNulos() {
        // Arrange
        AdsRequest request = new AdsRequest();
        request.setTitle(" ");
        request.setDescription(null);
        request.setPrice(BigDecimal.ZERO);
        request.setCategory(null);
        request.setImages(null);

        // Act
        var resposta = given()
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
        .when()
                .post("/api/ads");

        // Assert
        resposta.then()
                .statusCode(400)
                .body("error", equalTo("Requisição inválida"))
                .body("message", notNullValue())
                .body("message", not(emptyString()));
    }

    private AdsRequest criarRequisicaoValida() {
        AdsRequest request = new AdsRequest();
        request.setTitle(TITLE);
        request.setDescription(DESCRIPTION);
        request.setPrice(new BigDecimal("850.00"));
        request.setCategory(CATEGORY);
        request.setImages(List.of(IMAGE_URL));
        return request;
    }
}
