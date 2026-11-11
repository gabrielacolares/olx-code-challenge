package br.com.olx.anuncios.integration.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ModerationResponse {
    private String status;

    @Getter(AccessLevel.NONE)
    private String reason;

    @Getter(AccessLevel.NONE)
    private String rejectionReason;

    public String getReason() {
        return reason != null ? reason : rejectionReason;
    }
}
