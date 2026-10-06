package com.kinplatform.kin.health.documents.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kinplatform.common.event.InMemoryDomainEventBus;
import com.kinplatform.common.audit.api.AuditService;
import com.kinplatform.common.audit.config.AuditProperties;
import com.kinplatform.kin.health.documents.InMemoryClinicalDocumentRepository;
import com.kinplatform.kin.health.documents.config.DocumentProperties;
import com.kinplatform.kin.health.documents.event.DocumentUploadedEvent;
import com.kinplatform.kin.health.documents.infrastructure.DocumentStorage;
import com.kinplatform.kin.health.documents.port.DocumentStorageQuotaPort;
import com.kinplatform.kin.health.physician.InMemoryPhysicianRepositories;
import com.kinplatform.kin.health.physician.access.RelationshipAccessValidator;
import com.kinplatform.kin.health.physician.access.RelationshipNotActiveException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests del servicio de documentos clínicos (ADR-036): subida, listado,
 * descarga, eliminación y permisos por relación ACTIVE.
 */
class DocumentServiceTest {

    private static final UUID PHYSICIAN = UUID.randomUUID();
    private static final UUID PATIENT = UUID.randomUUID();

    private InMemoryClinicalDocumentRepository repository;
    private InMemoryDomainEventBus bus;
    private DocumentProperties properties;
    private DocumentService service;

    @BeforeEach
    void setUp() throws Exception {
        repository = new InMemoryClinicalDocumentRepository();
        bus = new InMemoryDomainEventBus();
        properties = new DocumentProperties();
        properties.setStoragePath(Files.createTempDirectory("kin-docs-test").toString());
        service = new DocumentService(
                repository,
                new DocumentStorage(properties),
                new RelationshipAccessValidator(activePhysicians().patientRepository()),
                auditService(false),
                properties,
                bus,
                null,
                permissiveStorageQuota());
    }

    private InMemoryPhysicianRepositories activePhysicians() {
        var physicians = new InMemoryPhysicianRepositories();
        physicians.patientRepository().assign(InMemoryPhysicianRepositories.assignment(PHYSICIAN, PATIENT));
        return physicians;
    }

    private static AuditService auditService(boolean enabled) {
        var auditProps = new AuditProperties();
        auditProps.setEnabled(enabled);
        return new AuditService(null, null, null, auditProps);
    }

    private static byte[] content(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private static DocumentStorageQuotaPort permissiveStorageQuota() {
        return new DocumentStorageQuotaPort() {
            @Override
            public long getStorageUsedBytes(UUID userId) {
                return 0;
            }

            @Override
            public long getStorageLimitBytes(UUID userId) {
                return Long.MAX_VALUE;
            }

            @Override
            public boolean canUpload(UUID userId, long fileSizeBytes) {
                return true;
            }
        };
    }

    @Test
    void uploadDocument_conRelacionActiva_deberiaSubir() {
        var doc = service.uploadDocument(PHYSICIAN, PATIENT, "resultado.pdf", "application/pdf",
                content("PDF"), "Resultado de laboratorio");

        assertEquals("resultado.pdf", doc.fileName());
        assertEquals("application/pdf", doc.mimeType());
        assertTrue(bus.publishedEvents().stream().anyMatch(e -> e instanceof DocumentUploadedEvent));
        assertEquals(1, service.listMyDocuments(PATIENT).size());
    }

    @Test
    void uploadDocument_sinRelacionActiva_deberiaLanzar() {
        var noRel = new InMemoryPhysicianRepositories();
        var service = new DocumentService(
                repository,
                new DocumentStorage(properties),
                new RelationshipAccessValidator(noRel.patientRepository()),
                auditService(false),
                properties,
                bus,
                null,
                permissiveStorageQuota());

        assertThrows(RelationshipNotActiveException.class,
                () -> service.uploadDocument(PHYSICIAN, PATIENT, "x.pdf", "application/pdf", content("x"), null));
    }

    @Test
    void uploadDocument_archivoVacio_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
                () -> service.uploadDocument(PHYSICIAN, PATIENT, "vacio.pdf", "application/pdf", new byte[0], null));
    }

    @Test
    void uploadDocument_archivoGrande_deberiaLanzar() {
        properties.setMaxFileSize(10);
        assertThrows(IllegalArgumentException.class,
                () -> service.uploadDocument(PHYSICIAN, PATIENT, "grande.pdf", "application/pdf",
                        content("muchos bytes"), null));
    }

    @Test
    void listDocumentsForPatient_deberiaDevolverDelPaciente() {
        service.uploadDocument(PHYSICIAN, PATIENT, "a.pdf", "application/pdf", content("a"), null);
        service.uploadDocument(PHYSICIAN, PATIENT, "b.pdf", "application/pdf", content("b"), null);

        assertEquals(2, service.listDocumentsForPatient(PHYSICIAN, PATIENT).size());
    }

    @Test
    void downloadDocument_pacienteDeberiaDescargar() {
        var doc = service.uploadDocument(PHYSICIAN, PATIENT, "a.pdf", "application/pdf", content("contenido"), null);

        var downloaded = service.downloadDocument(doc.id(), PATIENT);

        assertEquals("contenido", new String(downloaded.content(), StandardCharsets.UTF_8));
        assertEquals("a.pdf", downloaded.fileName());
    }

    @Test
    void downloadDocument_usuarioAjeno_deberiaLanzar() {
        var doc = service.uploadDocument(PHYSICIAN, PATIENT, "a.pdf", "application/pdf", content("x"), null);

        assertThrows(DocumentAccessDeniedException.class,
                () -> service.downloadDocument(doc.id(), UUID.randomUUID()));
    }

    @Test
    void deleteDocument_medicoQueSubio_deberiaEliminar() {
        var doc = service.uploadDocument(PHYSICIAN, PATIENT, "a.pdf", "application/pdf", content("x"), null);

        service.deleteDocument(doc.id(), PHYSICIAN);

        assertEquals(0, service.listMyDocuments(PATIENT).size());
        assertThrows(DocumentNotFoundException.class, () -> service.downloadDocument(doc.id(), PATIENT));
    }

    @Test
    void deleteDocument_otroUsuario_deberiaLanzar() {
        var doc = service.uploadDocument(PHYSICIAN, PATIENT, "a.pdf", "application/pdf", content("x"), null);

        assertThrows(DocumentAccessDeniedException.class,
                () -> service.deleteDocument(doc.id(), UUID.randomUUID()));
    }

    @Test
    void activeDocumentCountForPatient_deberiaContarSoloActivos() {
        var doc = service.uploadDocument(PHYSICIAN, PATIENT, "a.pdf", "application/pdf", content("x"), null);
        service.uploadDocument(PHYSICIAN, PATIENT, "b.pdf", "application/pdf", content("x"), null);
        service.deleteDocument(doc.id(), PHYSICIAN);

        assertEquals(1, service.activeDocumentCountForPatient(PATIENT));
    }

    @Test
    void conModuloDeshabilitado_deberiaLanzar() {
        properties.setEnabled(false);

        assertThrows(DocumentsDisabledException.class,
                () -> service.listMyDocuments(PATIENT));
    }
}


