package br.com.olx.anuncios.integration.moderation.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ModerationResponse {
    private String status;

    @JsonAlias("rejection_reason")
    private String reason;
}