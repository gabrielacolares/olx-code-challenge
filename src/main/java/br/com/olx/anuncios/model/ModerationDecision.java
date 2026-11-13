package br.com.olx.anuncios.model;

import lombok.Getter;

@Getter
public class ModerationDecision {
    private final AdsStatus status;
    private final String rejectionReason;

    public ModerationDecision(AdsStatus status, String rejectionReason) {
        this.status = status;
        this.rejectionReason = rejectionReason;
    }
}
