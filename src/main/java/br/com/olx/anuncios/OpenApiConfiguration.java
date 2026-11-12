package br.com.olx.anuncios;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {
    @Bean
    public OpenAPI adsApi() {
        return new OpenAPI().info(new Info()
                .title("API de Anúncios")
                .description("API REST para criar, consultar e listar anúncios.")
                .version("1.0.0"));
    }
}
