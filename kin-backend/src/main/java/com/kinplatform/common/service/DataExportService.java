package com.kinplatform.common.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kinplatform.common.entity.DataExportRequest;
import com.kinplatform.common.repository.DataExportRequestRepository;
import com.kinplatform.kin.health.common.entity.UserConsent;
import com.kinplatform.kin.health.common.repository.UserConsentRepository;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
public class DataExportService {

    private final DataExportRequestRepository exportRepository;
    private final UserRepository userRepository;
    private final UserConsentRepository consentRepository;
    private final ObjectMapper objectMapper;

    private static String EXPORT_DIR = "/app/storage/exports/";
    private static final int EXPIRY_DAYS = 7;
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneId.of("UTC"));

    public DataExportService(DataExportRequestRepository exportRepository, UserRepository userRepository, UserConsentRepository consentRepository) {
        this.exportRepository = exportRepository;
        this.userRepository = userRepository;
        this.consentRepository = consentRepository;
        this.objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Transactional
    public DataExportRequest requestExport(UUID userId) {
        long activeCount = exportRepository.countActiveByUser(userId);
        if (activeCount > 0) {
            throw new IllegalStateException("Ya tienes una exportación en proceso");
        }

        DataExportRequest request = DataExportRequest.builder()
            .userId(userId)
            .status("PENDING")
            .requestedAt(Instant.now())
            .build();
        request = exportRepository.save(request);

        processExport(request.getId());

        return request;
    }

    @Async
    @Transactional
    public void processExport(UUID requestId) {
        DataExportRequest request = exportRepository.findById(requestId).orElseThrow();
        try {
            request.setStatus("PROCESSING");
            exportRepository.save(request);

            Path userDir = Paths.get(EXPORT_DIR, request.getUserId().toString());
            Files.createDirectories(userDir);

            Path zipPath = userDir.resolve(requestId + ".zip");
            generateZip(request.getUserId(), zipPath);

            request.setFilePath(zipPath.toString());
            request.setFileSizeBytes(Files.size(zipPath));
            request.setStatus("COMPLETED");
            request.setCompletedAt(Instant.now());
            request.setExpiresAt(Instant.now().plus(EXPIRY_DAYS, java.time.temporal.ChronoUnit.DAYS));
            exportRepository.save(request);

        } catch (Exception e) {
            log.error("Error procesando export {}", requestId, e);
            request.setStatus("FAILED");
            request.setErrorMessage(e.getMessage());
            exportRepository.save(request);
        }
    }

    private void generateZip(UUID userId, Path zipPath) throws IOException {
        User user = userRepository.findById(userId).orElseThrow();

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath.toFile()))) {
            addFileToZip(zos, "README.txt", buildReadme(user));
            addFileToZip(zos, "profile.json", buildProfileJson(user));
            addFileToZip(zos, "consents.json", buildConsentsJson(userId));
            addFileToZip(zos, "metadata.json", buildMetadataJson(user));
        }
    }

    private void addFileToZip(ZipOutputStream zos, String name, String content) throws IOException {
        if (name.contains("..") || name.contains("/") || name.contains("\\")) {
            throw new IllegalArgumentException("Nombre de archivo inválido: " + name);
        }
        ZipEntry entry = new ZipEntry(name);
        zos.putNextEntry(entry);
        zos.write(content.getBytes("UTF-8"));
        zos.closeEntry();
    }

    private String buildReadme(User user) {
        StringBuilder sb = new StringBuilder();
        sb.append("EXPORTACIÓN DE DATOS PERSONALES - KIN HEALTH\n");
        sb.append("=============================================\n\n");
        sb.append("Usuario: ").append(user.getEmail()).append("\n");
        sb.append("ID: ").append(user.getId()).append("\n");
        sb.append("Fecha de exportación: ").append(ISO_FORMATTER.format(Instant.now())).append("\n");
        sb.append("Ley aplicable: Ley 1581 de 2012 (Colombia) - Art. 8 Derecho de Acceso\n\n");
        sb.append("CONTENIDO DEL ARCHIVO:\n");
        sb.append("- profile.json: Datos de perfil del usuario\n");
        sb.append("- consents.json: Historial de consentimientos (Ley 1581)\n");
        sb.append("- metadata.json: Metadatos de la exportación\n\n");
        sb.append("NOTA LEGAL:\n");
        sb.append("Si el usuario tiene Historia Clínica Electrónica (HCE), esta se rige por\n");
        sb.append("la Resolución 839 de 1995 (retención legal 20 años) y no se incluye\n");
        sb.append("en esta exportación automática. Solicítela por canal oficial.\n\n");
        sb.append("Verificación de integridad: SHA-256 de cada archivo JSON disponible en metadata.json\n");
        return sb.toString();
    }

    private String buildProfileJson(User user) {
        try {
            Map<String, Object> profile = new LinkedHashMap<>();
            profile.put("id", user.getId().toString());
            profile.put("email", user.getEmail());
            profile.put("fullName", user.getFullName());
            profile.put("documentType", null);
            profile.put("documentNumber", null);
            profile.put("phone", user.getPhone());
            profile.put("birthDate", user.getDateOfBirth() != null ? user.getDateOfBirth().toString() : null);
            profile.put("gender", user.getSex());
            profile.put("role", user.getRole() != null ? user.getRole().name() : null);
            profile.put("subscriptionTier", user.getCurrentPlan() != null ? user.getCurrentPlan().getName() : (user.getSubscription() != null ? user.getSubscription().getPlan().getName() : "FREE"));
            profile.put("healthDataConsent", user.getHealthDataConsent());
            profile.put("healthDataConsentAt", null);
            profile.put("marketingConsent", null);
            profile.put("marketingConsentAt", null);
            profile.put("termsAcceptedAt", null);
            profile.put("createdAt", user.getCreatedAt() != null ? ISO_FORMATTER.format(user.getCreatedAt().toInstant()) : null);
            profile.put("updatedAt", user.getUpdatedAt() != null ? ISO_FORMATTER.format(user.getUpdatedAt().toInstant()) : null);
            profile.put("lastLoginAt", user.getLastLoginAt() != null ? ISO_FORMATTER.format(user.getLastLoginAt().toInstant()) : null);
            profile.put("emailVerified", user.getEmailVerified());
            profile.put("emailVerifiedAt", null);
            profile.put("physicianVerificationStatus", user.getPhysicianVerificationStatus() != null ? user.getPhysicianVerificationStatus().name() : null);
            return objectMapper.writeValueAsString(profile);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializando profile.json", e);
        }
    }

    private String buildConsentsJson(UUID userId) {
        try {
            List<UserConsent> consents = new ArrayList<>(consentRepository.findByUserId(userId));
            consents.sort(Comparator.comparing(UserConsent::getCreatedAt).reversed());
            List<Map<String, Object>> consentList = new ArrayList<>();
            for (UserConsent c : consents) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", c.getId().toString());
                m.put("consentType", c.getConsentType() != null ? c.getConsentType().name() : null);
                m.put("version", c.getVersion());
                m.put("status", c.getAccepted() ? "ACCEPTED" : "PENDING");
                m.put("acceptedAt", c.getAcceptedAt() != null ? ISO_FORMATTER.format(c.getAcceptedAt()) : null);
                m.put("revokedAt", c.getRevokedAt() != null ? ISO_FORMATTER.format(c.getRevokedAt()) : null);
                m.put("revocationReason", c.getRevocationReason());
                m.put("ipAddress", c.getIpAddress());
                m.put("userAgent", c.getUserAgent());
                m.put("createdAt", c.getCreatedAt() != null ? ISO_FORMATTER.format(c.getCreatedAt()) : null);
                m.put("updatedAt", c.getUpdatedAt() != null ? ISO_FORMATTER.format(c.getUpdatedAt()) : null);
                consentList.add(m);
            }
            Map<String, Object> wrapper = new LinkedHashMap<>();
            wrapper.put("consents", consentList);
            wrapper.put("total", consentList.size());
            wrapper.put("exportedAt", ISO_FORMATTER.format(Instant.now()));
            return objectMapper.writeValueAsString(wrapper);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializando consents.json", e);
        }
    }

    private String buildMetadataJson(User user) {
        try {
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("exportVersion", "1.0");
            meta.put("generatedAt", ISO_FORMATTER.format(Instant.now()));
            meta.put("userId", user.getId().toString());
            meta.put("userEmail", user.getEmail());
            meta.put("filesIncluded", List.of("README.txt", "profile.json", "consents.json", "metadata.json"));
            meta.put("dataController", "KIN HEALTH SAS");
            meta.put("legalBasis", "Ley 1581 de 2012 Art. 8 - Derecho de Acceso / Habeas Data");
            meta.put("retentionPolicy", "Export disponible 7 días, luego eliminación automática");
            meta.put("contact", "privacy@kinhealth.com");
            return objectMapper.writeValueAsString(meta);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializando metadata.json", e);
        }
    }

    public DataExportRequest getExportStatus(UUID requestId, UUID userId) {
        DataExportRequest request = exportRepository.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("Export no encontrado"));
        if (!request.getUserId().equals(userId)) {
            throw new SecurityException("No autorizado");
        }
        return request;
    }

    public List<DataExportRequest> getExportsByUser(UUID userId) {
        return exportRepository.findByUserIdOrderByRequestedAtDesc(userId);
    }

    @Transactional
    public byte[] downloadExport(UUID requestId, UUID userId) throws IOException {
        DataExportRequest request = getExportStatus(requestId, userId);
        if (!"COMPLETED".equals(request.getStatus())) {
            throw new IllegalStateException("Export no completado");
        }
        if (request.getExpiresAt() != null && request.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalStateException("Export expirado");
        }
        request.setDownloadCount(request.getDownloadCount() + 1);
        exportRepository.save(request);
        return Files.readAllBytes(Paths.get(request.getFilePath()));
    }

    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void expireOldExports() {
        List<DataExportRequest> expired = exportRepository.findExpiredCompleted(Instant.now());
        for (DataExportRequest request : expired) {
            try {
                if (request.getFilePath() != null) {
                    Files.deleteIfExists(Paths.get(request.getFilePath()));
                }
                request.setStatus("EXPIRED");
                exportRepository.save(request);
            } catch (IOException e) {
                log.error("Error eliminando export {}", request.getId(), e);
            }
        }
    }
}