package br.com.olx.anuncios.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.olx.anuncios.mapper.AdsResponseMapper;
import br.com.olx.anuncios.model.Ads;
import br.com.olx.anuncios.model.AdsStatus;
import br.com.olx.anuncios.service.AdsService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdsControllerDetailTest {
    private static final String TITLE = "Urban bicycle, 29-inch wheels";
    private static final String DESCRIPTION = "Bicycle in great condition";
    private static final String IMAGE_URL = "https://img.test/bicicleta.jpg";

    private AdsService adsService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUpController() {
        adsService = mock(AdsService.class);

        AdsController controller = new AdsController();

        ReflectionTestUtils.setField(controller, "adsService", adsService);
        ReflectionTestUtils.setField(controller, "adsResponseMapper", Mappers.getMapper(AdsResponseMapper.class));

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void returns200AndAdWhenFound() throws Exception {
        UUID id = UUID.fromString("bb885686-0612-4d32-b8db-2d3dd519c2fd");

        Ads ad = new Ads(
                id,
                TITLE,
                DESCRIPTION,
                new BigDecimal("850.00"),
                "esportes",
                List.of(IMAGE_URL),
                AdsStatus.REJECTED,
                "prohibited_item");

        when(adsService.findById(id)).thenReturn(Optional.of(ad));

        mockMvc.perform(get("/api/ads/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.title").value(TITLE))
                .andExpect(jsonPath("$.description").value(DESCRIPTION))
                .andExpect(jsonPath("$.price").value(850.0))
                .andExpect(jsonPath("$.category").value("esportes"))
                .andExpect(jsonPath("$.images[0]").value(IMAGE_URL))
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("prohibited_item"));

        verify(adsService).findById(id);
    }

    @Test
    void returns404WhenAdDoesNotExist() throws Exception {
        UUID id = UUID.fromString("5a597312-ce13-4f9c-a091-742878ac45a9");

        when(adsService.findById(id)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/ads/{id}", id)).andExpect(status().isNotFound());

        verify(adsService).findById(id);
    }

    @Test
    void returns400WhenIdIsNotAValidUuid() throws Exception {
        mockMvc.perform(get("/api/ads/{id}", "invalid-id"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("bad_request"));
    }
}
