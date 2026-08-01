package com.kinplatform.ai.knowledge.adapter;

import com.kinplatform.kin.knowledge.KnowledgeCandidate;
import com.kinplatform.kin.knowledge.KnowledgeQuery;
import com.kinplatform.kin.knowledge.KnowledgeSource;
import com.kinplatform.kin.knowledge.engine.SourceValidator;

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

    public HttpKnowledgeSourceAdapter(String sourceId, String sourceName, String baseUrl,
                                      HttpClient client,
                                      Function<HttpResponse, List<HttpItem>> decoder) {
        this.sourceId = sourceId == null ? "" : sourceId;
        this.sourceName = sourceName == null ? "" : sourceName;
        this.baseUrl = baseUrl == null ? "" : baseUrl;
        this.client = client;
        this.decoder = decoder;
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
                    item.content(), sourceId, sourceName, item.url(), item.publishedAt(),
                    response.contentType(),
                    Map.of(SourceValidator.META_HTTP_STATUS, String.valueOf(response.statusCode()))));
            }
            return List.copyOf(candidates);
        } catch (RuntimeException ex) {
            return List.of();
        }
    }

    private HttpRequest buildRequest(KnowledgeQuery query) {
        String separator = baseUrl.indexOf('?') >= 0 ? "&" : "?";
        return new HttpRequest(baseUrl + separator + "q=" + query.topic(),
            Map.of("Accept", "application/json"));
    }
}
