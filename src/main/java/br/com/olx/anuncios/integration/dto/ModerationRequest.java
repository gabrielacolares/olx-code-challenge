package br.com.olx.anuncios.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class ModerationRequest {
    private final String title;
    private final String description;
    private final BigDecimal price;
    private final String category;
}
