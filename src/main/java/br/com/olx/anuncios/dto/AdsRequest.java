package br.com.olx.anuncios.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class AdsRequest {
    @NotBlank
    @Size(min = 10, max = 100)
    private String title;

    @NotBlank
    private String description;

    @NotNull
    @DecimalMin(value = "0", inclusive = false)
    private BigDecimal price;

    @NotBlank
    private String category;

    @NotEmpty
    private List<@NotBlank @Pattern(regexp = "^https?://\\S+$") String> images;
}
