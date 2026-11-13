package br.com.olx.anuncios.config;

import br.com.olx.anuncios.integration.moderation.http.ModerationApi;
import br.com.olx.anuncios.integration.moderation.http.ModerationRequestInterceptor;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class ModerationHttpConfiguration {
    @Bean("moderationRestClient")
    RestClient moderationRestClient(
            @Value("${moderation.base-url:http://localhost:8787}") String baseUrl,
            @Value("${moderation.http.connect-timeout:2s}") Duration connectTimeout,
            @Value("${moderation.http.response-timeout:10s}") Duration responseTimeout,
            ModerationRequestInterceptor moderationRequestInterceptor) {
        HttpClient httpClient =
                HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(responseTimeout);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .requestInterceptor(moderationRequestInterceptor)
                .build();
    }

    @Bean("moderationApi")
    ModerationApi moderationApi(@Qualifier("moderationRestClient") RestClient restClient) {
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build();
        return factory.createClient(ModerationApi.class);
    }
}
