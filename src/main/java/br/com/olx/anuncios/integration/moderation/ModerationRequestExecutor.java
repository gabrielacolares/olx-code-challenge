package br.com.olx.anuncios.integration.moderation;

import br.com.olx.anuncios.integration.moderation.dto.ModerationRequest;
import br.com.olx.anuncios.integration.moderation.dto.ModerationResponse;
import br.com.olx.anuncios.integration.moderation.http.ModerationApi;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Component;

@Component
public class ModerationRequestExecutor {
    private final ModerationApi moderationApi;

    public ModerationRequestExecutor(ModerationApi moderationApi) {
        this.moderationApi = moderationApi;
    }

    @Retry(name = "moderation")
    public ModerationResponse check(ModerationRequest request) {
        return moderationApi.check(request);
    }
}
