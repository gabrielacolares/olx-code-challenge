package br.com.olx.anuncios.integration.moderation.http;

import br.com.olx.anuncios.integration.moderation.dto.ModerationRequest;
import br.com.olx.anuncios.integration.moderation.dto.ModerationResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange(url = "/v1/moderation", contentType = MediaType.APPLICATION_JSON_VALUE)
public interface ModerationApi {
    @PostExchange("/check")
    ModerationResponse check(@RequestBody ModerationRequest request);
}
