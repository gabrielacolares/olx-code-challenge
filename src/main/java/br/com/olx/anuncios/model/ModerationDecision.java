package br.com.olx.anuncios.model;

import java.util.Objects;

public class ModerationDecision {
    private final AdsStatus status;
    private final String rejectionReason;

    public ModerationDecision(AdsStatus status, String rejectionReason) {
        this.status = Objects.requireNonNull(status, "status é obrigatório");

        if (status == AdsStatus.APPROVED && rejectionReason != null) {
            throw new IllegalArgumentException("anúncio aprovado não pode ter motivo de reprovação");
        }

        if (status == AdsStatus.REJECTED && (rejectionReason == null || rejectionReason.isBlank())) {
            throw new IllegalArgumentException("anúncio reprovado deve ter motivo de reprovação");
        }

        this.rejectionReason = rejectionReason;
    }

    public AdsStatus getStatus() {
        return status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }
}
