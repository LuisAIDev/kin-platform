package com.kinplatform.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuración del {@link WebClient} usado por los adapters HTTP de MIPRES
 * (perfil {@code prod}): {@code MipresHttpClient} y {@code MipresTokenService}.
 *
 * <p>Spring Boot no autoconfigura un bean de tipo {@code WebClient} (solo
 * {@code WebClient.Builder}), por lo que sin esta clase el arranque en
 * {@code prod} falla con {@code NoSuchBeanDefinitionException}.</p>
 *
 * <p>No exponemos un bean {@code WebClient.Builder} para no competir con el que
 * provee Spring Boot; construimos el {@code WebClient} directamente.</p>
 */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient(
            @Value("${mipres.base-url:https://wsmipres.sispro.gov.co/WSMIPRESNOPBS/}") String mipresBaseUrl) {
        return WebClient.builder()
                .baseUrl(mipresBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
