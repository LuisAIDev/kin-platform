package com.kinplatform.kin.enterprise.aggregate;

import com.kinplatform.kin.enterprise.valueobjects.DocumentArtifact;
import com.kinplatform.kin.enterprise.valueobjects.DocumentType;
import com.kinplatform.kin.enterprise.valueobjects.EnterpriseScore;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Aggregate root del proyecto empresarial de la Fase 10 (KIN Enterprise).
 *
 * <p>Agrega el conjunto de documentos generados para un proyecto de KIN a
 * partir de la conversación completada y gobierna su ciclo de vida. Cada
 * versión del proyecto empresarial es una instancia distinta e inmutable:
 * {@code version} identifica la versión dentro del mismo {@code projectId} y
 * la identidad del aggregate es {@code (projectId, version)}.</p>
 *
 * <p>Ciclo de vida (máquina de estados): {@code REQUESTED → RUNNING →
 * COMPLETED | FAILED}. Las transiciones se aplican mediante métodos de dominio
 * ({@link #startGeneration()}, {@link #completeGeneration()},
 * {@link #failGeneration(String)}) que devuelven una nueva instancia; las
 * transiciones no permitidas lanzan {@link EnterpriseProjectException} y el
 * aggregate nunca se modifica en el lugar (inmutabilidad total).</p>
 *
 * <p>Invariantes garantizadas por el constructor privado:</p>
 * <ul>
 *   <li>{@code projectId} no nulo.</li>
 *   <li>{@code version} mayor o igual a 1.</li>
 *   <li>{@code status} no nulo y coherente con el resto del estado.</li>
 *   <li>{@code createdAt} y {@code updatedAt} no nulos, con
 *       {@code updatedAt >= createdAt}.</li>
 *   <li>{@code completedAt} presente si y solo si el estado es
 *       {@code COMPLETED}.</li>
 *   <li>{@code failedReason} presente (no en blanco) si y solo si el estado es
 *       {@code FAILED}.</li>
 *   <li>{@code documents} nunca nulo, sin elementos nulos, sin duplicados por
 *       {@link DocumentType} y con un único tipo por documento.</li>
 *   <li>Un aggregate {@code REQUESTED} no puede portar documentos.</li>
 * </ul>
 *
 * <p>Los documentos solo pueden adjuntarse o reemplazarse mientras el proyecto
 * está en {@code RUNNING} (fase de generación); al completarse o fallar el
 * conjunto queda congelado. El método {@link #nextVersion()} crea una nueva
 * versión {@code REQUESTED} a partir de un estado terminal, sin modificar la
 * versión actual (que permanece inmutable).</p>
 */
public final class EnterpriseProject {

    private final UUID projectId;
    private final int version;
    private final GenerationStatus status;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;
    private final OffsetDateTime completedAt;
    private final String failedReason;
    private final List<DocumentArtifact> documents;
    private final EnterpriseScore score;

    /**
     * Constructor privado: punto único de validación de todas las invariantes.
     */
    private EnterpriseProject(UUID projectId, int version, GenerationStatus status,
                              OffsetDateTime createdAt, OffsetDateTime updatedAt,
                              OffsetDateTime completedAt, String failedReason,
                              List<DocumentArtifact> documents, EnterpriseScore score) {
        this.projectId = requireNotNull(projectId, "projectId");
        this.version = requireValidVersion(version);
        this.status = requireNotNull(status, "status");
        this.createdAt = requireNotNull(createdAt, "createdAt");
        this.updatedAt = requireNotNull(updatedAt, "updatedAt");
        requireConsistentTimestamps(this.createdAt, this.updatedAt);
        this.completedAt = validateCompletedAt(status, completedAt);
        this.failedReason = validateFailedReason(status, failedReason);
        this.documents = copyDocuments(documents);
        this.score = validateScore(status, score);
    }

    // ------------------------------------------------------------------
    // Fábricas estáticas (construcción y reconstrucción en un estado dado)
    // ------------------------------------------------------------------

    /**
     * Crea la versión inicial del proyecto empresarial en estado
     * {@code REQUESTED}, sin documentos y con timestamps actuales.
     *
     * @param projectId identificador del proyecto de KIN origen
     * @param version   versión solicitada (mayor o igual a 1)
     * @return agregado en estado {@code REQUESTED}
     * @throws EnterpriseProjectException si los argumentos violan las invariantes
     */
    public static EnterpriseProject request(UUID projectId, int version) {
        var now = OffsetDateTime.now();
        return new EnterpriseProject(projectId, version, GenerationStatus.REQUESTED,
            now, now, null, null, List.of(), null);
    }

    /**
     * Reconstruye un proyecto empresarial en estado {@code RUNNING}.
     *
     * <p>Fábrica de reconstrucción (p. ej. para un adaptador de persistencia)
     * y base de la transición {@link #startGeneration()}: valida que el estado
     * sea coherente con la ausencia de {@code completedAt}/{@code failedReason}.
     * Un aggregate {@code RUNNING} puede portar los documentos ya generados.</p>
     *
     * @param projectId identificador del proyecto de KIN origen
     * @param version   versión del proyecto (mayor o igual a 1)
     * @param createdAt instante de creación de la versión
     * @param updatedAt instante de la última actualización
     * @param documents documentos ya generados (sin duplicados por tipo)
     * @return agregado en estado {@code RUNNING}
     * @throws EnterpriseProjectException si los argumentos violan las invariantes
     */
    public static EnterpriseProject start(UUID projectId, int version,
                                          OffsetDateTime createdAt, OffsetDateTime updatedAt,
                                          List<DocumentArtifact> documents) {
        return start(projectId, version, createdAt, updatedAt, documents, null);
    }

    /**
     * Fábrica de reconstrucción {@code RUNNING} con el Enterprise Score ya
     * calculado (Fase 10, Milestone 3D).
     */
    public static EnterpriseProject start(UUID projectId, int version,
                                          OffsetDateTime createdAt, OffsetDateTime updatedAt,
                                          List<DocumentArtifact> documents,
                                          EnterpriseScore score) {
        return new EnterpriseProject(projectId, version, GenerationStatus.RUNNING,
            createdAt, updatedAt, null, null, documents, score);
    }

    /**
     * Reconstruye un proyecto empresarial completado (estado {@code COMPLETED}).
     *
     * <p>Fábrica de reconstrucción y base de la transición
     * {@link #completeGeneration()}. Requiere {@code completedAt} y prohíbe
     * {@code failedReason}.</p>
     *
     * @param projectId   identificador del proyecto de KIN origen
     * @param version     versión del proyecto (mayor o igual a 1)
     * @param createdAt   instante de creación de la versión
     * @param updatedAt   instante de la última actualización
     * @param completedAt instante de finalización de la generación
     * @param documents   documentos de la versión (congelados)
     * @return agregado en estado {@code COMPLETED}
     * @throws EnterpriseProjectException si los argumentos violan las invariantes
     */
    public static EnterpriseProject complete(UUID projectId, int version,
                                             OffsetDateTime createdAt, OffsetDateTime updatedAt,
                                             OffsetDateTime completedAt,
                                             List<DocumentArtifact> documents) {
        return complete(projectId, version, createdAt, updatedAt, completedAt, documents, null);
    }

    /**
     * Fábrica de reconstrucción {@code COMPLETED} con el Enterprise Score
     * persistido (Fase 10, Milestone 3D).
     */
    public static EnterpriseProject complete(UUID projectId, int version,
                                             OffsetDateTime createdAt, OffsetDateTime updatedAt,
                                             OffsetDateTime completedAt,
                                             List<DocumentArtifact> documents,
                                             EnterpriseScore score) {
        return new EnterpriseProject(projectId, version, GenerationStatus.COMPLETED,
            createdAt, updatedAt, completedAt, null, documents, score);
    }

    /**
     * Reconstruye un proyecto empresarial fallido (estado {@code FAILED}).
     *
     * <p>Fábrica de reconstrucción y base de la transición
     * {@link #failGeneration(String)}. Requiere un {@code failedReason} no en
     * blanco y prohíbe {@code completedAt}.</p>
     *
     * @param projectId    identificador del proyecto de KIN origen
     * @param version      versión del proyecto (mayor o igual a 1)
     * @param createdAt    instante de creación de la versión
     * @param updatedAt    instante de la última actualización
     * @param failedReason motivo del fallo (no en blanco)
     * @param documents    documentos parciales de la versión (congelados)
     * @return agregado en estado {@code FAILED}
     * @throws EnterpriseProjectException si los argumentos violan las invariantes
     */
    public static EnterpriseProject fail(UUID projectId, int version,
                                         OffsetDateTime createdAt, OffsetDateTime updatedAt,
                                         String failedReason, List<DocumentArtifact> documents) {
        return fail(projectId, version, createdAt, updatedAt, failedReason, documents, null);
    }

    /**
     * Fábrica de reconstrucción {@code FAILED} con el Enterprise Score parcial
     * persistido, si lo hubiera (Fase 10, Milestone 3D).
     */
    public static EnterpriseProject fail(UUID projectId, int version,
                                         OffsetDateTime createdAt, OffsetDateTime updatedAt,
                                         String failedReason, List<DocumentArtifact> documents,
                                         EnterpriseScore score) {
        return new EnterpriseProject(projectId, version, GenerationStatus.FAILED,
            createdAt, updatedAt, null, failedReason, documents, score);
    }

    // ------------------------------------------------------------------
    // Accesores
    // ------------------------------------------------------------------

    public UUID projectId() {
        return projectId;
    }

    public int version() {
        return version;
    }

    public GenerationStatus status() {
        return status;
    }

    public OffsetDateTime createdAt() {
        return createdAt;
    }

    public OffsetDateTime updatedAt() {
        return updatedAt;
    }

    public OffsetDateTime completedAt() {
        return completedAt;
    }

    public String failedReason() {
        return failedReason;
    }

    /**
     * Colección inmutable de documentos de la versión.
     */
    public List<DocumentArtifact> documents() {
        return documents;
    }

    /**
     * Enterprise Score de la versión, o {@code null} si aún no se calculó
     * (un aggregate {@code REQUESTED} nunca porta score).
     */
    public EnterpriseScore score() {
        return score;
    }

    // ------------------------------------------------------------------
    // Transiciones de la máquina de estados
    // ------------------------------------------------------------------

    /**
     * Transición {@code REQUESTED → RUNNING}: inicia la generación.
     *
     * @return nueva instancia en estado {@code RUNNING}, con el instante de
     *         actualización renovado
     * @throws EnterpriseProjectException si el estado actual no es
     *         {@code REQUESTED}
     */
    public EnterpriseProject startGeneration() {
        requireState(GenerationStatus.REQUESTED, "iniciar la generación");
        return EnterpriseProject.start(projectId, version, createdAt,
            OffsetDateTime.now(), documents, score);
    }

    /**
     * Transición {@code RUNNING → COMPLETED}: finaliza la generación con éxito.
     *
     * @return nueva instancia en estado {@code COMPLETED}, con
     *         {@code completedAt} fijado al instante actual
     * @throws EnterpriseProjectException si el estado actual no es
     *         {@code RUNNING}
     */
    public EnterpriseProject completeGeneration() {
        requireState(GenerationStatus.RUNNING, "completar la generación");
        var now = OffsetDateTime.now();
        return EnterpriseProject.complete(projectId, version, createdAt, now,
            now, documents, score);
    }

    /**
     * Transición {@code RUNNING → FAILED}: abandona la generación por error.
     *
     * @param reason motivo del fallo (no en blanco)
     * @return nueva instancia en estado {@code FAILED} con el motivo registrado
     * @throws EnterpriseProjectException si el estado actual no es
     *         {@code RUNNING} o si el motivo es nulo o en blanco
     */
    public EnterpriseProject failGeneration(String reason) {
        requireState(GenerationStatus.RUNNING, "abortar la generación");
        return EnterpriseProject.fail(projectId, version, createdAt,
            OffsetDateTime.now(), reason, documents, score);
    }

    // ------------------------------------------------------------------
    // Gestión de documentos (solo en estado RUNNING)
    // ------------------------------------------------------------------

    /**
     * Adjunta un documento nuevo al proyecto en generación.
     *
     * @param document documento a adjuntar (no nulo y sin tipo duplicado)
     * @return nueva instancia {@code RUNNING} con el documento añadido
     * @throws EnterpriseProjectException si el estado no es {@code RUNNING}, el
     *         documento es nulo o ya existe un documento del mismo tipo
     */
    public EnterpriseProject attachDocument(DocumentArtifact document) {
        requireState(GenerationStatus.RUNNING, "adjuntar un documento");
        if (document == null) {
            throw new EnterpriseProjectException(identity() + " no admite documentos nulos.");
        }
        if (hasDocument(document.type())) {
            throw new EnterpriseProjectException(identity() + " ya contiene un documento de tipo "
                + document.type() + ".");
        }
        var updated = new ArrayList<>(documents);
        updated.add(document);
        return EnterpriseProject.start(projectId, version, createdAt,
            OffsetDateTime.now(), updated, score);
    }

    /**
     * Reemplaza un documento existente por uno regenerado del mismo tipo.
     *
     * @param document documento que sustituye al existente (no nulo)
     * @return nueva instancia {@code RUNNING} con el documento reemplazado
     * @throws EnterpriseProjectException si el estado no es {@code RUNNING}, el
     *         documento es nulo o no existe un documento del mismo tipo que
     *         reemplazar
     */
    public EnterpriseProject replaceDocument(DocumentArtifact document) {
        requireState(GenerationStatus.RUNNING, "reemplazar un documento");
        if (document == null) {
            throw new EnterpriseProjectException(identity() + " no admite documentos nulos.");
        }
        int index = indexOf(document.type());
        if (index < 0) {
            throw new EnterpriseProjectException(identity() + " no contiene un documento de tipo "
                + document.type() + " para reemplazar.");
        }
        var updated = new ArrayList<>(documents);
        updated.set(index, document);
        return EnterpriseProject.start(projectId, version, createdAt,
            OffsetDateTime.now(), updated, score);
    }

    /**
     * Adjunta el Enterprise Score calculado a la versión en generación
     * (Fase 10, Milestone 3D). El score se calcula exclusivamente por el
     * {@code EnterpriseScoreEngine} y se conserva en las transiciones
     * posteriores ({@link #completeGeneration()}, {@link #failGeneration(String)}).
     *
     * @param score Enterprise Score calculado (no nulo)
     * @return nueva instancia {@code RUNNING} con el score adjuntado
     * @throws EnterpriseProjectException si el estado no es {@code RUNNING} o el
     *         score es nulo
     */
    public EnterpriseProject withScore(EnterpriseScore score) {
        requireState(GenerationStatus.RUNNING, "adjuntar el score");
        if (score == null) {
            throw new EnterpriseProjectException(identity() + " no admite un score nulo.");
        }
        return new EnterpriseProject(projectId, version, status, createdAt,
            OffsetDateTime.now(), completedAt, failedReason, documents, score);
    }

    /**
     * Indica si la versión contiene un documento del tipo dado.
     */
    public boolean hasDocument(DocumentType type) {
        return indexOf(requireNotNull(type, "type")) >= 0;
    }

    /**
     * Devuelve el documento del tipo dado, si existe.
     */
    public Optional<DocumentArtifact> findDocument(DocumentType type) {
        int index = indexOf(requireNotNull(type, "type"));
        return index >= 0 ? Optional.of(documents.get(index)) : Optional.empty();
    }

    /**
     * Número de documentos de la versión.
     */
    public int documentCount() {
        return documents.size();
    }

    // ------------------------------------------------------------------
    // Consultas de estado y regeneración
    // ------------------------------------------------------------------

    public boolean isRequested() {
        return status == GenerationStatus.REQUESTED;
    }

    public boolean isRunning() {
        return status == GenerationStatus.RUNNING;
    }

    public boolean isCompleted() {
        return status == GenerationStatus.COMPLETED;
    }

    public boolean isFailed() {
        return status == GenerationStatus.FAILED;
    }

    /**
     * Indica si la versión alcanzó un estado terminal ({@code COMPLETED} o
     * {@code FAILED}) desde el que puede generarse una nueva versión.
     */
    public boolean canRegenerate() {
        return isCompleted() || isFailed();
    }

    // ------------------------------------------------------------------
    // Versionado
    // ------------------------------------------------------------------

    /**
     * Genera la siguiente versión del proyecto empresarial a partir de un
     * estado terminal.
     *
     * <p>Devuelve una nueva instancia {@code REQUESTED} con {@code version + 1},
     * sin documentos y con timestamps actuales, lista para persistirse e
     * iniciar un nuevo ciclo de generación. La versión actual permanece
     * totalmente inmutable.</p>
     *
     * @return nueva instancia {@code REQUESTED} con la versión siguiente
     * @throws EnterpriseProjectException si el estado actual no es terminal
     *         (no puede regenerarse)
     */
    public EnterpriseProject nextVersion() {
        if (!canRegenerate()) {
            throw new EnterpriseProjectException(identity()
                + " no admite una nueva versión: solo desde un estado terminal "
                + "(COMPLETED o FAILED), actual: " + status + ".");
        }
        return EnterpriseProject.request(projectId, version + 1);
    }

    // ------------------------------------------------------------------
    // Identidad
    // ------------------------------------------------------------------

    /**
     * La identidad del aggregate es {@code (projectId, version)}: dos instancias
     * con la misma identidad representan la misma versión del proyecto aunque su
     * estado o documentos difieran (p. ej. antes y después de una transición).
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof EnterpriseProject that)) {
            return false;
        }
        return version == that.version && projectId.equals(that.projectId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectId, version);
    }

    @Override
    public String toString() {
        return "EnterpriseProject[" + projectId + ", v" + version + ", " + status
            + ", " + documents.size() + " documento(s)]";
    }

    // ------------------------------------------------------------------
    // Validación de invariantes
    // ------------------------------------------------------------------

    private void requireState(GenerationStatus expected, String operation) {
        if (status != expected) {
            throw new EnterpriseProjectException("No se puede " + operation + " en "
                + identity() + ": requiere estado " + expected + ", actual: " + status + ".");
        }
    }

    private String identity() {
        return "el proyecto empresarial (" + projectId + ", v" + version + ")";
    }

    private static <T> T requireNotNull(T value, String field) {
        if (value == null) {
            throw new EnterpriseProjectException("'" + field + "' del proyecto empresarial no puede ser nulo.");
        }
        return value;
    }

    private static int requireValidVersion(int value) {
        if (value < 1) {
            throw new EnterpriseProjectException("La versión del proyecto empresarial debe ser mayor o igual a 1 (recibida: "
                + value + ").");
        }
        return value;
    }

    private static void requireConsistentTimestamps(OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        if (updatedAt.isBefore(createdAt)) {
            throw new EnterpriseProjectException("'updatedAt' no puede ser anterior a 'createdAt'.");
        }
    }

    private static OffsetDateTime validateCompletedAt(GenerationStatus status, OffsetDateTime completedAt) {
        if (status == GenerationStatus.COMPLETED && completedAt == null) {
            throw new EnterpriseProjectException("Un proyecto COMPLETED debe tener 'completedAt'.");
        }
        if (status != GenerationStatus.COMPLETED && completedAt != null) {
            throw new EnterpriseProjectException("'completedAt' solo puede estar presente en estado COMPLETED (actual: "
                + status + ").");
        }
        return completedAt;
    }

    private static String validateFailedReason(GenerationStatus status, String failedReason) {
        if (status == GenerationStatus.FAILED
            && (failedReason == null || failedReason.isBlank())) {
            throw new EnterpriseProjectException("Un proyecto FAILED debe tener un 'failedReason' no en blanco.");
        }
        if (status != GenerationStatus.FAILED && failedReason != null) {
            throw new EnterpriseProjectException("'failedReason' solo puede estar presente en estado FAILED (actual: "
                + status + ").");
        }
        return failedReason;
    }

    private static EnterpriseScore validateScore(GenerationStatus status, EnterpriseScore score) {
        if (status == GenerationStatus.REQUESTED && score != null) {
            throw new EnterpriseProjectException(
                "Un proyecto REQUESTED no puede portar Enterprise Score (actual: " + status + ").");
        }
        return score;
    }

    private static List<DocumentArtifact> copyDocuments(List<DocumentArtifact> documents) {
        if (documents == null) {
            throw new EnterpriseProjectException("'documents' del proyecto empresarial no puede ser nulo.");
        }
        for (DocumentArtifact document : documents) {
            if (document == null) {
                throw new EnterpriseProjectException("'documents' no puede contener documentos nulos.");
            }
        }
        var types = new ArrayList<DocumentType>(documents.size());
        for (DocumentArtifact document : documents) {
            if (types.contains(document.type())) {
                throw new EnterpriseProjectException("'documents' no puede contener duplicados del tipo "
                    + document.type() + ".");
            }
            types.add(document.type());
        }
        return List.copyOf(documents);
    }

    private int indexOf(DocumentType type) {
        for (int i = 0; i < documents.size(); i++) {
            if (documents.get(i).type() == type) {
                return i;
            }
        }
        return -1;
    }
}
