package com.kinplatform.kin.health.telemedicine.config;

import com.kinplatform.kin.health.telemedicine.adapter.ContentCipher;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wiring del módulo de telemedicina (ADR-032).
 *
 * <p>Habilita {@link TelemedicineProperties} y declara el {@link ContentCipher}
 * para cifrar el contenido de mensajes en reposo. El resto de beans
 * ({@code TelemedicineService}, {@code TelemedicineController}) se
 * auto-descubren por componente.</p>
 */
@Configuration
@EnableConfigurationProperties(TelemedicineProperties.class)
public class TelemedicineConfig {

    @Bean
    public ContentCipher contentCipher(TelemedicineProperties properties) {
        return new ContentCipher(properties.getCryptoSecret());
    }
}
