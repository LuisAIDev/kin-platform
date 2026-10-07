package com.kinplatform.common.ai.knowledge.adapter;

import com.kinplatform.common.knowledge.KnowledgeCandidate;
import com.kinplatform.common.knowledge.KnowledgeQuery;
import com.kinplatform.common.knowledge.KnowledgeSource;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Fuente compuesta con filtro por categoría de proyecto (ADR-024, infraestructura).
 *
 * <p>Implementa el puerto {@link KnowledgeSource} consultando solo las fuentes
 * pertinentes a la categoría del proyecto: una fuente es pertinente si
 * {@code query.category()} está vacía (sin filtro, compatibilidad) o si la
 * fuente no declara categorías (contexto general) o si su lista de categorías
 * contiene la categoría del proyecto. <strong>Java decide</strong> qué fuentes
 * consultar; el LLM nunca participa ni ejecuta peticiones.</p>
 *
 * <p>Preserva el orden de registro (comportamiento determinista) y nunca ordena
 * ni filtra los candidatos por contenido (eso vive en el dominio,
 * {@code SourceValidator}).</p>
 */
public class CategoryAwareCompositeKnowledgeSource implements KnowledgeSource {

    /** Referencia interna: fuente + categorías a las que aplica (ADR-025: enabled/priority). */
    private static final class SourceRef {
        private final KnowledgeSource source;
        private final Set<String> categories;
        private final boolean enabled;
        private final int priority;

        SourceRef(KnowledgeSource source, Set<String> categories, boolean enabled, int priority) {
            this.source = source;
            this.categories = categories == null ? Set.of() : lowerCaseCopy(categories);
            this.enabled = enabled;
            this.priority = priority;
        }

        boolean matches(String category) {
            if (!enabled) {
                return false; // ADR-025: fuente deshabilitada no se consulta
            }
            if (categories.isEmpty()) {
                return true; // contexto general
            }
            if (category == null || category.isBlank()) {
                return true; // sin filtro de categoría → todas
            }
            return categories.contains(normalize(category));
        }
    }

    private final List<SourceRef> refs;

    /** Constructor de compatibilidad: todas habilitadas, prioridad 0 (orden de configuración). */
    public CategoryAwareCompositeKnowledgeSource(
            List<KnowledgeSource> sources, List<List<String>> categoriesPerSource) {
        this(sources, categoriesPerSource, null, null);
    }

    /**
     * Constructor con política por fuente (ADR-025): habilitación y prioridad. Las
     * fuentes se ordenan por prioridad descendente (empate conserva el orden de
     * configuración, determinista).
     */
    public CategoryAwareCompositeKnowledgeSource(
            List<KnowledgeSource> sources,
            List<List<String>> categoriesPerSource,
            List<Boolean> enabled,
            List<Integer> priorities) {
        var refs = new ArrayList<SourceRef>();
        if (sources != null) {
            for (int i = 0; i < sources.size(); i++) {
                KnowledgeSource source = sources.get(i);
                if (source == null) {
                    continue;
                }
                Set<String> cats = (categoriesPerSource == null || i >= categoriesPerSource.size())
                        ? Set.of()
                        : new LinkedHashSet<>(
                                categoriesPerSource.get(i) == null ? List.of() : categoriesPerSource.get(i));
                boolean on = enabled == null || i >= enabled.size() || enabled.get(i) == null || enabled.get(i);
                int prio = (priorities == null || i >= priorities.size() || priorities.get(i) == null)
                        ? 0
                        : priorities.get(i);
                refs.add(new SourceRef(source, cats, on, prio));
            }
        }
        refs.sort((a, b) -> Integer.compare(b.priority, a.priority)); // estable: empate conserva orden
        this.refs = List.copyOf(refs);
    }

    @Override
    public List<KnowledgeCandidate> fetch(KnowledgeQuery query) {
        if (query == null) {
            return List.of();
        }
        var candidates = new ArrayList<KnowledgeCandidate>();
        for (SourceRef ref : refs) {
            if (ref.matches(query.category())) {
                var fetched = ref.source.fetch(query);
                if (fetched != null) {
                    candidates.addAll(fetched);
                }
            }
        }
        return List.copyOf(candidates);
    }

    private static Set<String> lowerCaseCopy(Set<String> values) {
        var out = new LinkedHashSet<String>();
        for (var value : values) {
            if (value != null && !value.isBlank()) {
                out.add(normalize(value));
            }
        }
        return Set.copyOf(out);
    }

    /**
     * Normaliza una categoría para la comparación: minúsculas y sin acentos
     * (p. ej. {@code "Logística"} → {@code "logistica"}). Evita que el nombre
     * de la categoría difiera del código por acentos/espacios.
     */
    private static String normalize(String value) {
        String lower = value.trim().toLowerCase(Locale.ROOT);
        return java.text.Normalizer.normalize(lower, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }
}


