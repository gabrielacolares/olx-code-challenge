package br.com.olx.anuncios.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Dados para criar um anuncio")
public class AdsRequest {
    @NotBlank
    @Size(min = 10, max = 100)
    @Schema(description = "Titulo entre 10 e 100 caracteres", example = "Bicicleta urbana aro 29")
    private String title;

    @NotBlank
    @Schema(description = "Descricao do anuncio", example = "Bicicleta em otimo estado")
    private String description;

    @NotNull
    @DecimalMin(value = "0", inclusive = false)
    @Schema(description = "Preco maior que zero", example = "850.00", minimum = "0.01")
    private BigDecimal price;

    @NotBlank
    @Schema(description = "Categoria do anuncio", example = "esportes")
    private String category;

    @NotEmpty
    @Schema(description = "Uma ou mais URLs HTTP(S) das imagens")
    private List<@NotBlank @Pattern(regexp = "^https?://\\S+$") String> images;
}
