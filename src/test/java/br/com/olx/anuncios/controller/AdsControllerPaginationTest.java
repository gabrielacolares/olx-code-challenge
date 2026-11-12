package br.com.olx.anuncios.controller;

import br.com.olx.anuncios.mapper.AdsResponseMapper;
import br.com.olx.anuncios.model.Ads;
import br.com.olx.anuncios.model.AdsPage;
import br.com.olx.anuncios.model.AdsStatus;
import br.com.olx.anuncios.service.AdsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdsControllerPaginationTest {
    private AdsService adsService;
    private MockMvc mockMvc;

    @BeforeEach
    void prepararController() {
        adsService = mock(AdsService.class);
        AdsController controller = new AdsController();
        ReflectionTestUtils.setField(controller, "adsService", adsService);
        ReflectionTestUtils.setField(controller, "adsResponseMapper", Mappers.getMapper(AdsResponseMapper.class));
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void deveUsarParametrosPadraoEExibirMetadados() throws Exception {
        Ads anuncio = anuncio(1, "esportes");
        when(adsService.list(0, 20, null)).thenReturn(new AdsPage(List.of(anuncio), 0, 20, 1, 1));

        mockMvc.perform(get("/api/ads"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(anuncio.getId().toString()))
                .andExpect(jsonPath("$.items[0].title").value(anuncio.getTitle()))
                .andExpect(jsonPath("$.items[0].images[0]").value("https://img.test/ad.jpg"))
                .andExpect(jsonPath("$.items[0].status").value("APPROVED"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(adsService).list(0, 20, null);
    }

    @Test
    void deveUsarParametrosExplicitosEFiltrarCategoria() throws Exception {
        Ads anuncio = anuncio(2, "esportes");
        when(adsService.list(1, 5, "esportes"))
                .thenReturn(new AdsPage(List.of(anuncio), 1, 5, 6, 2));

        mockMvc.perform(get("/api/ads").param("page", "1").param("size", "5").param("category", "esportes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].category").value("esportes"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalItems").value(6))
                .andExpect(jsonPath("$.totalPages").value(2));

        verify(adsService).list(1, 5, "esportes");
    }

    @Test
    void deveRetornar400ParaParametroDePaginacaoInvalido() throws Exception {
        when(adsService.list(-1, 20, null)).thenThrow(new IllegalArgumentException("invalid page"));

        mockMvc.perform(get("/api/ads").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        mockMvc.perform(get("/api/ads").param("size", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    private Ads anuncio(int sequence, String category) {
        UUID id = UUID.fromString(String.format("00000000-0000-0000-0000-%012d", sequence));
        return new Ads(id, "Anuncio de teste", "Descricao do anuncio", new BigDecimal("25.00"),
                category, List.of("https://img.test/ad.jpg"), AdsStatus.APPROVED, null);
    }
}
