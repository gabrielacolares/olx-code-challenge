package br.com.olx.anuncios.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Schema(description = "Erro retornado pela API")
public class ErrorResponse {

    @Schema(description = "Código identificador do erro", example = "validation_error")
    private final String error;

    @Schema(description = "Mensagem resumida do erro", example = "Erro de validação nos campos")
    private final String message;

    @Schema(
            description = "Lista detalhada de erros por campo, quando aplicável",
            example = "[\"title: tamanho deve ser entre 10 e 100\"]")
    private final List<String> details;

    public ErrorResponse(String error, String message) {
        this(error, message, List.of());
    }
}
