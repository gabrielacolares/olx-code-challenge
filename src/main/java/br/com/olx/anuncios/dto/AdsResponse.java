package br.com.olx.anuncios.dto;

import br.com.olx.anuncios.model.AdsStatus;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
public class AdsResponse {
    private final UUID id;
    private final String title;
    private final String description;
    private final BigDecimal price;
    private final String category;
    private final List<String> images;
    private final AdsStatus status;
    private final String rejectionReason;

    public AdsResponse(UUID id, String title, String description, BigDecimal price, String category,
                       List<String> images, AdsStatus status, String rejectionReason) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.price = price;
        this.category = category;
        this.images = List.copyOf(images);
        this.status = status;
        this.rejectionReason = rejectionReason;
    }
}
