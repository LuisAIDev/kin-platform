package com.kinplatform.ai.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

/**
 * Valida el contrato de configuración real (application.yml) para las
 * capacidades de OpenTelemetry (ADR-022) y conocimiento externo (ADR-021).
 *
 * <p>Carga el archivo real sin levantar el contexto completo: verifica que el
 * tracing está deshabilitado por defecto (sin collector obligatorio), que la
 * exportación OTLP es configurable por variable de entorno, y que la
 * adquisición de conocimiento es offline-first por defecto.</p>
 */
class TracingConfigurationTest {

    private static Properties loadApplicationYaml() {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource("application.yml"));
        factory.afterPropertiesSet();
        return factory.getObject();
    }

    @Test
    void tracing_deberiaEstarDeshabilitadoPorDefecto() {
        Properties props = loadApplicationYaml();

        assertFalse(Boolean.parseBoolean(props.getProperty("management.tracing.enabled")));
        assertEquals("0.0", props.getProperty("management.tracing.sampling.probability"));
        assertTrue(props.containsKey("management.otlp.tracing.endpoint"));
    }

    @Test
    void otlp_deberiaSerConfigurablePorVariableDeEntorno() {
        Properties props = loadApplicationYaml();

        assertEquals("${OTEL_EXPORTER_OTLP_ENDPOINT:}", props.getProperty("management.otlp.tracing.endpoint"));
    }

    @Test
    void conocimientoExterno_deberiaSerOfflineFirstPorDefecto() {
        Properties props = loadApplicationYaml();

        assertFalse(Boolean.parseBoolean(props.getProperty("kin.knowledge.external-enabled")));
        assertEquals("${KNOWLEDGE_ALLOWED_DOMAINS:}", props.getProperty("kin.knowledge.allowed-domains"));
        assertFalse(Boolean.parseBoolean(props.getProperty("kin.knowledge.test-source.enabled")));
        assertFalse(Boolean.parseBoolean(props.getProperty("kin.knowledge.http.allow-loopback")));
    }
}
