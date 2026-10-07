package com.kinplatform.common.ai.knowledge.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kinplatform.common.ai.knowledge.adapter.SourceConnectionGuard.HostResolver;
import com.kinplatform.common.ai.knowledge.adapter.SourceConnectionGuard.Validation;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SourceConnectionGuardTest {

    private static HostResolver resolver(Map<String, InetAddress[]> hosts) {
        return host -> {
            InetAddress[] addresses = hosts.get(host.toLowerCase(java.util.Locale.ROOT));
            if (addresses == null) {
                throw new UnknownHostException(host);
            }
            return addresses;
        };
    }

    private static InetAddress ip(String literal) {
        try {
            return InetAddress.getByName(literal);
        } catch (UnknownHostException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Test
    void allowlist_deberiaPermitirDominioExactoYSubdominio() {
        var resolver = resolver(Map.of(
                "autorizado.com", new InetAddress[] {ip("8.8.8.8")},
                "api.autorizado.com", new InetAddress[] {ip("8.8.8.8")}));
        SourceConnectionGuard guard = new SourceConnectionGuard(Set.of("autorizado.com"), false, resolver);

        assertTrue(guard.validate("https://autorizado.com/search").allowed());
        assertTrue(guard.validate("https://api.autorizado.com/search").allowed());
        assertFalse(guard.validate("https://evil.com/search").allowed());
        assertFalse(guard.validate("https://autorizado.com.evil.com/search").allowed());
        assertFalse(guard.validate("https://notautorizado.com/search").allowed());
    }

    @Test
    void allowlist_deberiaSerCaseInsensitive() {
        var resolver = resolver(Map.of("api.autorizado.com", new InetAddress[] {ip("8.8.8.8")}));
        SourceConnectionGuard guard = new SourceConnectionGuard(Set.of("AUTORIZADO.com"), false, resolver);

        assertTrue(guard.validate("https://API.AUTORIZADO.com/search").allowed());
    }

    @Test
    void allowlistVacia_deberiaRechazarTodoFueraDeLoopback() {
        var resolver = resolver(Map.of("public.com", new InetAddress[] {ip("8.8.8.8")}));
        SourceConnectionGuard guard = new SourceConnectionGuard(Set.of(), false, resolver);

        assertFalse(guard.validate("https://public.com/x").allowed());
        assertEquals(
                "Dominio no permitido por allowlist",
                guard.validate("https://public.com/x").reason());
    }

    @Test
    void protocolo_deberiaRequerirHttps() {
        var resolver = resolver(Map.of("autorizado.com", new InetAddress[] {ip("8.8.8.8")}));
        SourceConnectionGuard guard = new SourceConnectionGuard(Set.of("autorizado.com"), false, resolver);

        assertFalse(guard.validate("http://autorizado.com/x").allowed());
        assertFalse(guard.validate("file:///etc/passwd").allowed());
        assertFalse(guard.validate("ftp://autorizado.com/x").allowed());
        assertFalse(guard.validate("gopher://autorizado.com/x").allowed());
        assertFalse(guard.validate("data:text/plain,hi").allowed());
        assertTrue(guard.validate("https://autorizado.com/x").allowed());
    }

    @Test
    void literalesDeIp_deberianRechazarse() {
        var resolver = resolver(Map.of("8.8.8.8", new InetAddress[] {ip("8.8.8.8")}));
        SourceConnectionGuard guard = new SourceConnectionGuard(Set.of("8.8.8.8"), false, resolver);

        assertFalse(guard.validate("https://8.8.8.8/x").allowed());
        assertFalse(guard.validate("https://[::1]/x").allowed());
    }

    @Test
    void rangosPrivadosYReservados_deberianRechazarse() {
        var resolver = resolver(new HashMap<>(Map.of(
                "h10.com", new InetAddress[] {ip("10.0.0.1")},
                "h172.com", new InetAddress[] {ip("172.16.0.1")},
                "h192.com", new InetAddress[] {ip("192.168.1.1")},
                "h127.com", new InetAddress[] {ip("127.0.0.1")},
                "h169.com", new InetAddress[] {ip("169.254.169.254")},
                "h0.com", new InetAddress[] {ip("0.0.0.0")},
                "h100.com", new InetAddress[] {ip("100.64.0.1")},
                "h198.com", new InetAddress[] {ip("198.18.0.1")},
                "h192b.com", new InetAddress[] {ip("192.0.0.9")},
                "h224.com", new InetAddress[] {ip("224.0.0.1")})));
        SourceConnectionGuard guard = new SourceConnectionGuard(
                Set.of(
                        "h10.com",
                        "h172.com",
                        "h192.com",
                        "h127.com",
                        "h169.com",
                        "h0.com",
                        "h100.com",
                        "h198.com",
                        "h192b.com",
                        "h224.com"),
                false,
                resolver);

        for (String host : new String[] {
            "h10.com",
            "h172.com",
            "h192.com",
            "h127.com",
            "h169.com",
            "h0.com",
            "h100.com",
            "h198.com",
            "h192b.com",
            "h224.com"
        }) {
            assertFalse(guard.validate("https://" + host + "/x").allowed(), "debería rechazar " + host);
            assertEquals(
                    "Dirección resuelta en rango privado o reservado",
                    guard.validate("https://" + host + "/x").reason());
        }
    }

    @Test
    void ipPublica_deberiaPermitirse() {
        var resolver = resolver(Map.of("public.com", new InetAddress[] {ip("8.8.8.8")}));
        SourceConnectionGuard guard = new SourceConnectionGuard(Set.of("public.com"), false, resolver);

        assertTrue(guard.validate("https://public.com/x").allowed());
    }

    @Test
    void resolucionMixta_conUnIpPrivado_deberiaFallarCerrado() {
        var resolver = resolver(Map.of("mixto.com", new InetAddress[] {ip("8.8.8.8"), ip("10.0.0.1")}));
        SourceConnectionGuard guard = new SourceConnectionGuard(Set.of("mixto.com"), false, resolver);

        Validation validation = guard.validate("https://mixto.com/x");
        assertFalse(validation.allowed());
        assertEquals("Dirección resuelta en rango privado o reservado", validation.reason());
    }

    @Test
    void resolucionFallida_deberiaRechazar() {
        SourceConnectionGuard guard = new SourceConnectionGuard(Set.of("inexistente.com"), false, resolver(Map.of()));

        Validation validation = guard.validate("https://inexistente.com/x");
        assertFalse(validation.allowed());
        assertEquals("Resolución DNS fallida o sin direcciones públicas", validation.reason());
    }

    @Test
    void loopback_deberiaPermitirseSoloCuandoSeOptaExplicitamente() {
        var resolver = resolver(Map.of("localhost", new InetAddress[] {ip("127.0.0.1")}));
        SourceConnectionGuard estricto = new SourceConnectionGuard(Set.of(), false, resolver);
        SourceConnectionGuard laxo = new SourceConnectionGuard(Set.of(), true, resolver);

        assertFalse(estricto.validate("http://localhost:8080/x").allowed());
        assertFalse(laxo.validate("https://public.com/x").allowed());
        assertTrue(laxo.validate("http://localhost:8080/x").allowed());
        assertTrue(laxo.allowsLoopback());
        assertFalse(estricto.allowsLoopback());
    }

    @Test
    void urlMalformada_deberiaRechazarse() {
        SourceConnectionGuard guard = new SourceConnectionGuard(Set.of("a.com"), false, resolver(Map.of()));

        Validation validation = guard.validate("no es una url");
        assertFalse(validation.allowed());
        assertNull(validation.uri());
    }
}


