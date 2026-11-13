package br.com.olx.anuncios.dto;

import br.com.olx.anuncios.model.AdsStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Anuncio retornado pela API")
public class AdsResponse {
    private final UUID id;
    private final String title;
    private final String description;
    private final BigDecimal price;
    private final String category;
    private final List<String> images;
    private final AdsStatus status;
    private final String rejectionReason;
}
