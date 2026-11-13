package br.com.olx.anuncios.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Resultado paginado da listagem de anuncios")
public class AdsPageResponse {
    private final List<AdsResponse> items;
    private final int page;
    private final int size;
    private final long totalItems;
    private final int totalPages;
}
