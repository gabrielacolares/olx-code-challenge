package br.com.olx.anuncios.integration.moderation.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ModerationRequest {
    private final String title;
    private final String description;
    private final BigDecimal price;
    private final String category;
}
