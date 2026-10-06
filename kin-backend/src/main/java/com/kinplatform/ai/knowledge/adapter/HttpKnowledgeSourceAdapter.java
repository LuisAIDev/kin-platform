package com.kinplatform.ai.knowledge.adapter;

import com.kinplatform.common.knowledge.KnowledgeCandidate;
import com.kinplatform.common.knowledge.KnowledgeQuery;
import com.kinplatform.common.knowledge.KnowledgeSource;
import com.kinplatform.common.knowledge.engine.SourceValidator;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Adaptador de infraestructura HTTP (ADR-014, §6.1/§6.6): implementa el puerto
 * {@link KnowledgeSource} sin tocar la red.
 *
 * <p>Estructura preparada: no realiza llamadas reales a Internet. El cliente
 * HTTP es una {@link HttpClient} (interfaz/stub) inyectada y el mapeo del cuerpo
 * de la respuesta a ítems crudos lo hace un {@code decoder} inyectado. El
 * adaptador únicamente envuelve los ítems crudos en {@link KnowledgeCandidate}
 * estampando su identidad ({@code sourceId}/{@code sourceName}) y los atributos
 * facturables de la respuesta (estado HTTP y tipo de contenido); nunca valida,
 * nunca decide ni interpreta negocio (offline-first: ante error o nulo degrada a
 * lista vacía).</p>
 */
public class HttpKnowledgeSourceAdapter implements KnowledgeSource {

    /** Cliente HTTP de infraestructura (stub; sin implementación funcional). */
    public interface HttpClient {
        HttpResponse fetch(HttpRequest request);
    }

    /** Solicitud HTTP cruda enviada al cliente. */
    public record HttpRequest(String url, Map<String, String> headers) {

        public HttpRequest {
            url = url == null ? "" : url;
            headers = headers == null ? Map.of() : Map.copyOf(headers);
        }
    }

    /** Respuesta HTTP cruda devuelta por el cliente. */
    public record HttpResponse(int statusCode, String contentType, String body) {

        public HttpResponse {
            contentType = contentType == null ? "" : contentType;
            body = body == null ? "" : body;
        }
    }

    /** Ítem crudo extraído del cuerpo de la respuesta por el decoder. */
    public record HttpItem(String content, String url, OffsetDateTime publishedAt) {

        public HttpItem {
            content = content == null ? "" : content;
            url = url == null ? "" : url;
        }
    }

    private final String sourceId;
    private final String sourceName;
    private final String baseUrl;
    private final HttpClient client;
    private final Function<HttpResponse, List<HttpItem>> decoder;
    /** Parámetro de consulta anexado con el tema (p. ej. {@code q}); vacío = sin sufijo. */
    private final String queryParam;
    /** Ventana de frescura de la fuente (ADR-025); se estampa en cada candidato. */
    private final java.time.Duration maxAge;

    public HttpKnowledgeSourceAdapter(
            String sourceId,
            String sourceName,
            String baseUrl,
            HttpClient client,
            Function<HttpResponse, List<HttpItem>> decoder) {
        this(sourceId, sourceName, baseUrl, client, decoder, "q");
    }

    /**
     * @param queryParam nombre del parámetro que transporta el tema; {@code null}
     *                   o vacío omite el sufijo (fuentes con consulta fija, p. ej.
     *                   SODA/Socrata que rechazan argumentos desconocidos).
     */
    public HttpKnowledgeSourceAdapter(
            String sourceId,
            String sourceName,
            String baseUrl,
            HttpClient client,
            Function<HttpResponse, List<HttpItem>> decoder,
            String queryParam) {
        this(sourceId, sourceName, baseUrl, client, decoder, queryParam, null);
    }

    /**
     * @param maxAge ventana de frescura de la fuente (TTL de caché sugerido,
     *               ADR-025); {@code null} = el llamador usa la ventana de la request.
     */
    public HttpKnowledgeSourceAdapter(
            String sourceId,
            String sourceName,
            String baseUrl,
            HttpClient client,
            Function<HttpResponse, List<HttpItem>> decoder,
            String queryParam,
            java.time.Duration maxAge) {
        this.sourceId = sourceId == null ? "" : sourceId;
        this.sourceName = sourceName == null ? "" : sourceName;
        this.baseUrl = baseUrl == null ? "" : baseUrl;
        this.client = client;
        this.decoder = decoder;
        this.queryParam = queryParam == null ? "" : queryParam.trim();
        this.maxAge = maxAge;
    }

    @Override
    public List<KnowledgeCandidate> fetch(KnowledgeQuery query) {
        if (query == null) {
            return List.of();
        }
        try {
            HttpResponse response = client.fetch(buildRequest(query));
            if (response == null || decoder == null) {
                return List.of();
            }
            List<HttpItem> items = decoder.apply(response);
            if (items == null) {
                return List.of();
            }
            var candidates = new ArrayList<KnowledgeCandidate>();
            for (var item : items) {
                if (item == null) {
                    continue;
                }
                candidates.add(new KnowledgeCandidate(
                        item.content(),
                        sourceId,
                        sourceName,
                        item.url(),
                        item.publishedAt(),
                        response.contentType(),
                        Map.of(SourceValidator.META_HTTP_STATUS, String.valueOf(response.statusCode())),
                        maxAge));
            }
            return List.copyOf(candidates);
        } catch (RuntimeException ex) {
            return List.of();
        }
    }

    private HttpRequest buildRequest(KnowledgeQuery query) {
        if (queryParam.isEmpty()) {
            return new HttpRequest(baseUrl, Map.of("Accept", "application/json"));
        }
        String separator = baseUrl.indexOf('?') >= 0 ? "&" : "?";
        return new HttpRequest(
                baseUrl + separator + queryParam + "=" + query.topic(), Map.of("Accept", "application/json"));
    }
}

