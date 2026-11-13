package br.com.olx.anuncios.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.Data;

@Data
@Entity
@Table(name = "ads")
public class Ads {

    @Id
    private UUID id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 40)
    private String category;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "ad_images", joinColumns = @JoinColumn(name = "ad_id"))
    @OrderColumn(name = "image_order")
    @Column(name = "image_url", nullable = false, length = 2048)
    private List<String> images = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdsStatus status;

    @Column(length = 100)
    private String rejectionReason;

    protected Ads() {}

    public Ads(UUID id, String title, String description, BigDecimal price, String category, List<String> images) {
        this(id, title, description, price, category, images, AdsStatus.PENDING, null);
    }

    public Ads(
            UUID id,
            String title,
            String description,
            BigDecimal price,
            String category,
            List<String> images,
            AdsStatus status,
            String rejectionReason) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.price = price;
        this.category = category;
        this.images = images == null ? new ArrayList<>() : new ArrayList<>(images);
        this.status = status;
        this.rejectionReason = rejectionReason;

        validate();
        validateModerationDecision();
    }

    public void validate() {
        Objects.requireNonNull(id, "id é obrigatório");

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("título é obrigatório e não pode ser vazio");
        }

        int titleLength = title.codePointCount(0, title.length());
        if (titleLength < 10 || titleLength > 100) {
            throw new IllegalArgumentException("título deve ter entre 10 e 100 caracteres");
        }

        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("descrição é obrigatória");
        }

        if (price == null || price.signum() <= 0) {
            throw new IllegalArgumentException("preço deve ser maior que zero");
        }

        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("categoria é obrigatória");
        }

        if (images.isEmpty()) {
            throw new IllegalArgumentException("ao menos uma imagem é obrigatória");
        }

        if (images.stream().anyMatch(image -> image == null || image.isBlank())) {
            throw new IllegalArgumentException("imagem não pode ser vazia");
        }
    }

    public void applyModerationDecision(AdsStatus status, String rejectionReason) {
        this.status = status;
        this.rejectionReason = rejectionReason;

        validateModerationDecision();
    }

    private void validateModerationDecision() {
        Objects.requireNonNull(status, "status é obrigatório");

        if (status == AdsStatus.APPROVED && rejectionReason != null) {
            throw new IllegalArgumentException("anúncio aprovado não pode ter motivo de reprovação");
        }

        if (status == AdsStatus.REJECTED && (rejectionReason == null || rejectionReason.isBlank())) {
            throw new IllegalArgumentException("anúncio reprovado deve ter motivo de reprovação");
        }
    }
}
