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
@Schema(description = "Dados para criar um anúncio")
public class AdsRequest {
    @NotBlank
    @Size(min = 10, max = 100)
    @Schema(description = "Título entre 10 e 100 caracteres", example = "Bicicleta urbana aro 29")
    private String title;

    @NotBlank
    @Schema(description = "Descrição do anúncio", example = "Bicicleta em ótimo estado")
    private String description;

    @NotNull
    @DecimalMin(value = "0", inclusive = false)
    @Schema(description = "Preço maior que zero", example = "850.00", minimum = "0.01")
    private BigDecimal price;

    @NotBlank
    @Pattern(
            regexp = "^(eletronicos|moveis|veiculos|imoveis|esportes|colecionaveis)$",
            message = "categoria deve ser uma das categorias permitidas")
    @Schema(
            description = "Categoria do anúncio",
            example = "esportes",
            allowableValues = {"eletronicos", "moveis", "veiculos", "imoveis", "esportes", "colecionaveis"})
    private String category;

    @NotEmpty
    @Schema(description = "Uma ou mais URLs HTTP(S) das imagens")
    private List<@NotBlank @Pattern(regexp = "^https?://\\S+$") String> images;
}
