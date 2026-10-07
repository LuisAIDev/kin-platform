package com.kinplatform.common.ai.knowledge.adapter;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Guardia de conexión SSRF-safe para adaptadores HTTP de conocimiento (ADR-021).
 *
 * <p>Vive en infraestructura (nunca en el dominio). Valida en Java, antes de
 * cualquier conexión y en cada salto de redirección, que el destino esté
 * permitido por allowlist y no apunte a rangos privados, loopback, link-local,
 * site-local, multicast ni reservados. Es <em>fail-closed</em>: cualquier duda
 * rechaza el destino.</p>
 *
 * <p>Reglas aplicadas sobre la URL de conexión:</p>
 * <ul>
 *   <li>protocolo HTTPS obligatorio; HTTP solo se permite para hosts loopback
 *       cuando {@code allowLoopback} está habilitado explícitamente
 *       (dev/test controlado, nunca por defecto);</li>
 *   <li>el host debe ser un nombre de dominio (las literales de IP se rechazan
 *       siempre);</li>
 *   <li>el host debe coincidir con la allowlist (igual o subdominio);
 *       si la allowlist está vacía, solo se admiten hosts loopback cuando
 *       {@code allowLoopback} está habilitado (offline-first por defecto);</li>
 *   <li>resolución DNS: TODAS las direcciones resueltas deben ser públicas;
 *       si alguna cae en un rango prohibido, se rechaza (mitigación de
 *       DNS-rebinding por resolución, fail-closed);</li>
 *   <li>las redirecciones se validan con las mismas reglas (ADR-021).</li>
 * </ul>
 */
public final class SourceConnectionGuard {

    /** Resolutor de hosts inyectable (tests deterministas sin red). */
    @FunctionalInterface
    public interface HostResolver {
        InetAddress[] resolve(String host) throws UnknownHostException;
    }

    /** Resultado de la validación de un destino de conexión. */
    public record Validation(boolean allowed, URI uri, String reason) {
        static Validation allowed(URI uri) {
            return new Validation(true, uri, "");
        }

        static Validation rejected(String reason) {
            return new Validation(false, null, reason);
        }
    }

    private static final String REASON_PROTOCOL = "Protocolo HTTPS obligatorio (o HTTP solo para loopback explícito)";
    private static final String REASON_HOST = "Host inválido o vacío";
    private static final String REASON_IP_LITERAL = "Las literales de IP no se permiten como host";
    private static final String REASON_DOMAIN = "Dominio no permitido por allowlist";
    private static final String REASON_RESOLUTION = "Resolución DNS fallida o sin direcciones públicas";
    private static final String REASON_PRIVATE_IP = "Dirección resuelta en rango privado o reservado";

    private final Set<String> allowedDomains;
    private final boolean allowLoopback;
    private final HostResolver resolver;

    public SourceConnectionGuard(Set<String> allowedDomains, boolean allowLoopback, HostResolver resolver) {
        this.allowedDomains = lowerCaseCopy(allowedDomains);
        this.allowLoopback = allowLoopback;
        this.resolver = resolver == null ? this::resolveDefault : resolver;
    }

    public SourceConnectionGuard(Set<String> allowedDomains, boolean allowLoopback) {
        this(allowedDomains, allowLoopback, null);
    }

    /**
     * Valida una URL absoluta de conexión (también usable para cada salto de
     * redirección). Devuelve {@link Validation#allowed(URI)} o el motivo de
     * rechazo; nunca lanza para URLs malformadas.
     */
    public Validation validate(String url) {
        if (url == null || url.isBlank()) {
            return Validation.rejected(REASON_HOST);
        }
        URI uri;
        try {
            uri = URI.create(url.trim());
        } catch (RuntimeException ex) {
            return Validation.rejected(REASON_HOST);
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!"https".equals(scheme)) {
            String hostForProtocol = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            if (!("http".equals(scheme) && allowLoopback && isLoopbackHost(hostForProtocol))) {
                return Validation.rejected(REASON_PROTOCOL);
            }
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            return Validation.rejected(REASON_HOST);
        }
        host = stripTrailingDot(host.toLowerCase(Locale.ROOT));
        if (isIpLiteral(host)) {
            return Validation.rejected(REASON_IP_LITERAL);
        }
        boolean loopbackHost = allowLoopback && isLoopbackHost(host);
        if (!loopbackHost && !domainAllowed(host)) {
            return Validation.rejected(REASON_DOMAIN);
        }
        InetAddress[] addresses = resolve(host);
        if (addresses == null || addresses.length == 0) {
            return Validation.rejected(REASON_RESOLUTION);
        }
        for (InetAddress address : addresses) {
            if (isForbiddenAddress(address)) {
                return Validation.rejected(REASON_PRIVATE_IP);
            }
        }
        return Validation.allowed(uri);
    }

    /** Indica si el guard permite conexiones loopback explícitas (solo dev/test). */
    public boolean allowsLoopback() {
        return allowLoopback;
    }

    private boolean domainAllowed(String host) {
        if (allowedDomains.isEmpty()) {
            return false;
        }
        for (String domain : allowedDomains) {
            if (domain.isBlank()) {
                continue;
            }
            if (host.equals(domain) || host.endsWith("." + domain)) {
                return true;
            }
        }
        return false;
    }

    private boolean isLoopbackHost(String host) {
        return "localhost".equals(host) || host.endsWith(".localhost") || host.endsWith(".localtest.me");
    }

    private boolean isIpLiteral(String host) {
        return host.indexOf(':') >= 0 || host.matches("[0-9.]+");
    }

    private InetAddress[] resolve(String host) {
        try {
            return resolver.resolve(host);
        } catch (UnknownHostException ex) {
            return new InetAddress[0];
        }
    }

    private InetAddress[] resolveDefault(String host) throws UnknownHostException {
        return InetAddress.getAllByName(host);
    }

    private boolean isForbiddenAddress(InetAddress address) {
        if (allowLoopback && address.isLoopbackAddress()) {
            return false;
        }
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }
        byte[] bytes = address.getAddress();
        if (bytes.length != 4) {
            return false;
        }
        int first = bytes[0] & 0xFF;
        int second = bytes[1] & 0xFF;
        if (first == 0) {
            return true;
        }
        if (first == 100 && (second & 0xC0) == 0x40) {
            return true; // 100.64.0.0/10 CGNAT
        }
        if (first == 192 && second == 0) {
            return true; // 192.0.0.0/24 IETF protocol assignments
        }
        if (first == 198 && (second & 0xFE) == 0x12) {
            return true; // 198.18.0.0/15 benchmark
        }
        return first >= 224; // multicast / reserved
    }

    private Set<String> lowerCaseCopy(Set<String> values) {
        if (values == null) {
            return Set.of();
        }
        var out = new LinkedHashSet<String>();
        for (var value : values) {
            if (value != null && !value.isBlank()) {
                out.add(stripTrailingDot(value.trim().toLowerCase(Locale.ROOT)));
            }
        }
        return Set.copyOf(out);
    }

    private String stripTrailingDot(String host) {
        return host.endsWith(".") ? host.substring(0, host.length() - 1) : host;
    }
}

