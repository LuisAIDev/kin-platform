package com.kinplatform.kin.health.performance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kinplatform.kin.health.triage.InMemoryTriageKnowledgeRepository;
import com.kinplatform.kin.health.triage.domain.Condition;
import com.kinplatform.kin.health.triage.domain.Severity;
import com.kinplatform.kin.health.triage.domain.Symptom;
import com.kinplatform.kin.health.triage.domain.SymptomConditionRelation;
import com.kinplatform.kin.health.triage.domain.TriageCatalog;
import com.kinplatform.kin.health.triage.domain.TriageInput;
import com.kinplatform.kin.health.triage.domain.Urgency;
import com.kinplatform.kin.health.triage.engine.TriageEngine;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * Prueba de carga determinista (fase de producción): 100 usuarios concurrentes
 * ejecutando el motor de triaje (cálculo 100 % Java, sin I/O) y validando que
 * la latencia agregada se mantiene acotada. No depende de infraestructura
 * externa; sirve como smoke test de concurrencia del motor.
 */
class TriageConcurrencyLoadTest {

    private static final UUID C1 = UUID.fromString("22220000-0000-0000-0000-000000010002");
    private static final UUID C2 = UUID.fromString("22220000-0000-0000-0000-000000010010");
    private static final UUID S1 = UUID.fromString("22220000-0000-0000-0000-000000000001");
    private static final UUID S2 = UUID.fromString("22220000-0000-0000-0000-000000000002");
    private static final UUID S3 = UUID.fromString("22220000-0000-0000-0000-000000000006");

    @Test
    void cienUsuariosConcurrentes_deberianCompletarSinError() throws Exception {
        var catalog = new TriageCatalog(
                List.of(
                        Symptom.of(S1, "fiebre", "T", null),
                        Symptom.of(S2, "tos", "T", null),
                        Symptom.of(S3, "dolor de cabeza", "T", null)),
                List.of(
                        Condition.of(C1, "Gripe", "D", "J11", Severity.MODERADO, Urgency.MEDIA, "R"),
                        Condition.of(C2, "Asma", "D", "J45", Severity.GRAVE, Urgency.ALTA, "R")),
                List.of(
                        SymptomConditionRelation.of(S1, C1, 0.9, true),
                        SymptomConditionRelation.of(S2, C1, 0.6, false),
                        SymptomConditionRelation.of(S3, C1, 0.5, false),
                        SymptomConditionRelation.of(S2, C2, 0.5, false),
                        SymptomConditionRelation.of(S1, C2, 0.4, false)));
        var engine = new TriageEngine(new InMemoryTriageKnowledgeRepository(catalog));

        int users = 100;
        ExecutorService executor = Executors.newFixedThreadPool(users);
        try {
            List<Callable<Boolean>> tasks = new ArrayList<>();
            for (int i = 0; i < users; i++) {
                tasks.add(() -> {
                    var result = engine.evaluate(TriageInput.of(List.of("fiebre", "tos")));
                    return !result.isEmpty() && result.results().size() >= 1;
                });
            }
            long start = System.nanoTime();
            var futures = executor.invokeAll(tasks, 10, TimeUnit.SECONDS);
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

            long success = futures.stream()
                    .filter(f -> {
                        try {
                            return f.get(5, TimeUnit.SECONDS);
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .count();

            assertEquals(users, success, "todos los usuarios deben completar el triaje");
            // Umbral amplio: 100 evaluaciones puras deberían completarse en << 10 s.
            assertTrue(elapsedMs < 10_000, "latencia agregada fuera de rango: " + elapsedMs + "ms");
        } finally {
            executor.shutdown();
        }
    }
}
