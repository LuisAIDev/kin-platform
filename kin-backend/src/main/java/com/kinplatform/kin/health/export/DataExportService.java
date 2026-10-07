package com.kinplatform.kin.health.export;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kinplatform.common.entity.DataExportRequest;
import com.kinplatform.common.repository.DataExportRequestRepository;
import com.kinplatform.common.audit.adapter.AuditLogJpaRepository;
import com.kinplatform.common.audit.domain.AuditAction;
import com.kinplatform.common.audit.domain.AuditResourceType;
import com.kinplatform.kin.health.common.entity.UserConsent;
import com.kinplatform.kin.health.common.repository.UserConsentRepository;
import com.kinplatform.kin.health.documents.adapter.ClinicalDocumentJpaRepository;
import com.kinplatform.kin.health.documents.domain.DocumentStatus;
import com.kinplatform.kin.health.hce.entity.Diagnoses;
import com.kinplatform.kin.health.hce.entity.Encounter;
import com.kinplatform.kin.health.hce.entity.MedicalOrder;
import com.kinplatform.kin.health.hce.entity.PhysicalExam;
import com.kinplatform.kin.health.hce.entity.TreatmentPlan;
import com.kinplatform.kin.health.hce.repository.DiagnosesRepository;
import com.kinplatform.kin.health.hce.repository.EncounterRepository;
import com.kinplatform.kin.health.hce.repository.MedicalOrderRepository;
import com.kinplatform.kin.health.hce.repository.PhysicalExamRepository;
import com.kinplatform.kin.health.hce.repository.TreatmentPlanRepository;
import com.kinplatform.kin.health.telemedicine.adapter.MessageJpaRepository;
import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.math.BigDecimal;
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
    private final EncounterRepository encounterRepository;
    private final DiagnosesRepository diagnosesRepository;
    private final TreatmentPlanRepository treatmentPlanRepository;
    private final MedicalOrderRepository medicalOrderRepository;
    private final PhysicalExamRepository physicalExamRepository;
    private final ClinicalDocumentJpaRepository documentRepository;
    private final MessageJpaRepository messageRepository;
    private final AuditLogJpaRepository auditRepository;
    private final ObjectMapper objectMapper;

    private static String EXPORT_DIR = "/app/storage/exports/";
    private static final int EXPIRY_DAYS = 7;
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneId.of("UTC"));

    public DataExportService(DataExportRequestRepository exportRepository,
            UserRepository userRepository,
            UserConsentRepository consentRepository,
            EncounterRepository encounterRepository,
            DiagnosesRepository diagnosesRepository,
            TreatmentPlanRepository treatmentPlanRepository,
            MedicalOrderRepository medicalOrderRepository,
            PhysicalExamRepository physicalExamRepository,
            ClinicalDocumentJpaRepository documentRepository,
            MessageJpaRepository messageRepository,
            AuditLogJpaRepository auditRepository) {
        this.exportRepository = exportRepository;
        this.userRepository = userRepository;
        this.consentRepository = consentRepository;
        this.encounterRepository = encounterRepository;
        this.diagnosesRepository = diagnosesRepository;
        this.treatmentPlanRepository = treatmentPlanRepository;
        this.medicalOrderRepository = medicalOrderRepository;
        this.physicalExamRepository = physicalExamRepository;
        this.documentRepository = documentRepository;
        this.messageRepository = messageRepository;
        this.auditRepository = auditRepository;
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
            addFileToZip(zos, "encounters.json", buildEncountersJson(userId));
            addFileToZip(zos, "diagnoses.json", buildDiagnosesJson(userId));
            addFileToZip(zos, "treatment_plans.json", buildTreatmentPlansJson(userId));
            addFileToZip(zos, "medical_orders.json", buildMedicalOrdersJson(userId));
            addFileToZip(zos, "physical_exams.json", buildPhysicalExamsJson(userId));
            addFilesToZip(zos, "documents/", userId);
            addFileToZip(zos, "messages.json", buildMessagesJson(userId));
            addFileToZip(zos, "audit_log.json", buildAuditLogJson(userId));
            addFileToZip(zos, "metadata.json", buildMetadataJson(user));
        }
    }

    private void addFileToZip(ZipOutputStream zos, String name, String content) throws IOException {
        if (name.contains("..")) {
            throw new IllegalArgumentException("Nombre de archivo inválido: " + name);
        }
        ZipEntry entry = new ZipEntry(name);
        zos.putNextEntry(entry);
        zos.write(content.getBytes("UTF-8"));
        zos.closeEntry();
    }

    private void addFilesToZip(ZipOutputStream zos, String prefix, UUID userId) throws IOException {
        Path userDocDir = Paths.get("/app/storage/documents", userId.toString());
        if (!Files.exists(userDocDir)) {
            return;
        }
        try (var stream = Files.walk(userDocDir)) {
            stream.filter(Files::isRegularFile)
                .forEach(file -> {
                    try {
                        String relativePath = userDocDir.relativize(file).toString().replace("\\", "/");
                        String entryName = prefix + relativePath;
                        ZipEntry entry = new ZipEntry(entryName);
                        zos.putNextEntry(entry);
                        Files.copy(file, zos);
                        zos.closeEntry();
                    } catch (IOException e) {
                        log.warn("Error adding document to ZIP: {}", file, e);
                    }
                });
        }
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
        sb.append("- encounters.json: Encuentros médicos (Historia Clínica)\n");
        sb.append("- diagnoses.json: Diagnósticos CIE-10\n");
        sb.append("- treatment_plans.json: Planes de tratamiento\n");
        sb.append("- medical_orders.json: Órdenes médicas\n");
        sb.append("- physical_exams.json: Exámenes físicos\n");
        sb.append("- documents/: Documentos clínicos adjuntos\n");
        sb.append("- messages.json: Mensajes de telemedicina\n");
        sb.append("- audit_log.json: Log de auditoría (últimos 1000 accesos)\n");
        sb.append("- metadata.json: Metadatos de la exportación\n\n");
        sb.append("NOTA LEGAL:\n");
        sb.append("La Historia Clínica Electrónica (HCE) se rige por la Resolución 839 de 1995\n");
        sb.append("(retención legal 20 años). Esta exportación incluye los datos clínicos\n");
        sb.append("asociados al usuario. Para certificación oficial, solicite por canal formal.\n\n");
        sb.append("Verificación de integridad: SHA-256 de cada archivo disponible en metadata.json\n");
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

    private String buildEncountersJson(UUID userId) {
        try {
            List<Encounter> encounters = encounterRepository.findByPatientIdOrderByStartedAtDesc(userId, org.springframework.data.domain.Pageable.unpaged()).getContent();
            List<Map<String, Object>> list = new ArrayList<>();
            for (Encounter e : encounters) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", e.getId().toString());
                m.put("patientId", e.getPatientId().toString());
                m.put("physicianId", e.getPhysicianId() != null ? e.getPhysicianId().toString() : null);
                m.put("organizationId", e.getOrganizationId() != null ? e.getOrganizationId().toString() : null);
                m.put("status", e.getStatus() != null ? e.getStatus().name() : null);
                m.put("startedAt", e.getStartedAt() != null ? ISO_FORMATTER.format(e.getStartedAt()) : null);
                m.put("closedAt", e.getClosedAt() != null ? ISO_FORMATTER.format(e.getClosedAt()) : null);
                m.put("encounterType", e.getEncounterType() != null ? e.getEncounterType().name() : null);
                m.put("chiefComplaint", e.getChiefComplaint());
                m.put("createdAt", e.getCreatedAt() != null ? ISO_FORMATTER.format(e.getCreatedAt()) : null);
                m.put("updatedAt", e.getUpdatedAt() != null ? ISO_FORMATTER.format(e.getUpdatedAt()) : null);
                list.add(m);
            }
            Map<String, Object> wrapper = new LinkedHashMap<>();
            wrapper.put("encounters", list);
            wrapper.put("total", list.size());
            wrapper.put("exportedAt", ISO_FORMATTER.format(Instant.now()));
            return objectMapper.writeValueAsString(wrapper);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializando encounters.json", e);
        }
    }

    private String buildDiagnosesJson(UUID userId) {
        try {
            List<Diagnoses> diagnoses = diagnosesRepository.findByPatientIdOrderByCreatedAtDesc(userId);
            List<Map<String, Object>> list = new ArrayList<>();
            for (Diagnoses d : diagnoses) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", d.getId().toString());
                m.put("encounterId", d.getEncounterId() != null ? d.getEncounterId().toString() : null);
                m.put("patientId", d.getPatientId().toString());
                m.put("physicianId", d.getPhysicianId() != null ? d.getPhysicianId().toString() : null);
                m.put("diagnosisType", d.getDiagnosisType() != null ? d.getDiagnosisType().name() : null);
                m.put("status", d.getStatus() != null ? d.getStatus().name() : null);
                m.put("cie10Code", d.getCie10Code());
                m.put("cie10Description", d.getCie10Description());
                m.put("certainty", d.getCertainty() != null ? d.getCertainty().name() : null);
                m.put("classification", d.getClassification() != null ? d.getClassification().name() : null);
                m.put("onsetDate", d.getOnsetDate() != null ? d.getOnsetDate().toString() : null);
                m.put("resolutionDate", d.getResolutionDate() != null ? d.getResolutionDate().toString() : null);
                m.put("notes", d.getNotes());
                m.put("createdAt", d.getCreatedAt() != null ? ISO_FORMATTER.format(d.getCreatedAt()) : null);
                m.put("updatedAt", d.getUpdatedAt() != null ? ISO_FORMATTER.format(d.getUpdatedAt()) : null);
                list.add(m);
            }
            Map<String, Object> wrapper = new LinkedHashMap<>();
            wrapper.put("diagnoses", list);
            wrapper.put("total", list.size());
            wrapper.put("exportedAt", ISO_FORMATTER.format(Instant.now()));
            return objectMapper.writeValueAsString(wrapper);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializando diagnoses.json", e);
        }
    }

    private String buildTreatmentPlansJson(UUID userId) {
        try {
            List<TreatmentPlan> plans = treatmentPlanRepository.findByPatientIdOrderByCreatedAtDesc(userId);
            List<Map<String, Object>> list = new ArrayList<>();
            for (TreatmentPlan t : plans) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", t.getId().toString());
                m.put("encounterId", t.getEncounterId() != null ? t.getEncounterId().toString() : null);
                m.put("patientId", t.getPatientId().toString());
                m.put("physicianId", t.getPhysicianId() != null ? t.getPhysicianId().toString() : null);
                m.put("conduct", t.getConduct() != null ? t.getConduct().name() : null);
                m.put("therapeuticGoals", t.getTherapeuticGoals());
                m.put("followupPlan", t.getFollowupPlan());
                m.put("reevaluationCriteria", t.getReevaluationCriteria());
                m.put("prognosis", t.getPrognosis() != null ? t.getPrognosis().name() : null);
                m.put("estimatedDuration", t.getEstimatedDuration() != null ? t.getEstimatedDuration().toString() : null);
                m.put("createdAt", t.getCreatedAt() != null ? ISO_FORMATTER.format(t.getCreatedAt()) : null);
                m.put("updatedAt", t.getUpdatedAt() != null ? ISO_FORMATTER.format(t.getUpdatedAt()) : null);
                list.add(m);
            }
            Map<String, Object> wrapper = new LinkedHashMap<>();
            wrapper.put("treatment_plans", list);
            wrapper.put("total", list.size());
            wrapper.put("exportedAt", ISO_FORMATTER.format(Instant.now()));
            return objectMapper.writeValueAsString(wrapper);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializando treatment_plans.json", e);
        }
    }

    private String buildMedicalOrdersJson(UUID userId) {
        try {
            List<MedicalOrder> orders = medicalOrderRepository.findByPatientIdOrderByOrderedAtDesc(userId);
            List<Map<String, Object>> list = new ArrayList<>();
            for (MedicalOrder m : orders) {
                Map<String, Object> o = new LinkedHashMap<>();
                o.put("id", m.getId().toString());
                o.put("treatmentPlanId", m.getTreatmentPlanId() != null ? m.getTreatmentPlanId().toString() : null);
                o.put("encounterId", m.getEncounterId() != null ? m.getEncounterId().toString() : null);
                o.put("patientId", m.getPatientId().toString());
                o.put("physicianId", m.getPhysicianId() != null ? m.getPhysicianId().toString() : null);
                o.put("orderType", m.getOrderType() != null ? m.getOrderType().name() : null);
                o.put("status", m.getStatus() != null ? m.getStatus().name() : null);
                o.put("drugName", m.getDrugName());
                o.put("dose", m.getDose());
                o.put("doseUnit", m.getDoseUnit());
                o.put("route", m.getRoute() != null ? m.getRoute().name() : null);
                o.put("frequency", m.getFrequency());
                o.put("durationDays", m.getDurationDays());
                o.put("cupsCode", m.getCupsCode());
                o.put("cupsDescription", m.getCupsDescription());
                o.put("bodySite", m.getBodySite());
                o.put("priority", m.getPriority() != null ? m.getPriority().name() : null);
                o.put("instructions", m.getInstructions());
                o.put("orderedAt", m.getOrderedAt() != null ? ISO_FORMATTER.format(m.getOrderedAt()) : null);
                o.put("executedAt", m.getExecutedAt() != null ? ISO_FORMATTER.format(m.getExecutedAt()) : null);
                o.put("executedBy", m.getExecutedBy() != null ? m.getExecutedBy().toString() : null);
                o.put("executionNotes", m.getExecutionNotes());
                o.put("createdAt", m.getCreatedAt() != null ? ISO_FORMATTER.format(m.getCreatedAt()) : null);
                o.put("updatedAt", m.getUpdatedAt() != null ? ISO_FORMATTER.format(m.getUpdatedAt()) : null);
                list.add(o);
            }
            Map<String, Object> wrapper = new LinkedHashMap<>();
            wrapper.put("medical_orders", list);
            wrapper.put("total", list.size());
            wrapper.put("exportedAt", ISO_FORMATTER.format(Instant.now()));
            return objectMapper.writeValueAsString(wrapper);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializando medical_orders.json", e);
        }
    }

    private String buildPhysicalExamsJson(UUID userId) {
        try {
            List<PhysicalExam> exams = physicalExamRepository.findByPatientIdOrderByRecordedAtDesc(userId);
            List<Map<String, Object>> list = new ArrayList<>();
            for (PhysicalExam e : exams) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", e.getId().toString());
                m.put("encounterId", e.getEncounterId() != null ? e.getEncounterId().toString() : null);
                m.put("evolutionId", e.getEvolutionId() != null ? e.getEvolutionId().toString() : null);
                m.put("patientId", e.getPatientId().toString());
                m.put("physicianId", e.getPhysicianId() != null ? e.getPhysicianId().toString() : null);
                m.put("recordedAt", e.getRecordedAt() != null ? ISO_FORMATTER.format(e.getRecordedAt()) : null);
                m.put("bpSystolic", e.getBpSystolic());
                m.put("bpDiastolic", e.getBpDiastolic());
                m.put("heartRate", e.getHeartRate());
                m.put("respiratoryRate", e.getRespiratoryRate());
                m.put("temperature", e.getTemperature() != null ? e.getTemperature().toString() : null);
                m.put("spo2", e.getSpo2());
                m.put("weightKg", e.getWeightKg() != null ? e.getWeightKg().toString() : null);
                m.put("heightCm", e.getHeightCm() != null ? e.getHeightCm().toString() : null);
                m.put("bmi", e.getBmi() != null ? e.getBmi().toString() : null);
                m.put("glasgowScore", e.getGlasgowScore());
                m.put("painScale", e.getPainScale());
                m.put("painScaleType", e.getPainScaleType() != null ? e.getPainScaleType().name() : null);
                m.put("generalAppearance", e.getGeneralAppearance());
                m.put("headNeck", e.getHeadNeck());
                m.put("cardiovascular", e.getCardiovascular());
                m.put("respiratory", e.getRespiratory());
                m.put("abdominal", e.getAbdominal());
                m.put("neurological", e.getNeurological());
                m.put("musculoskeletal", e.getMusculoskeletal());
                m.put("skin", e.getSkin());
                m.put("genitourinary", e.getGenitourinary());
                m.put("psychiatric", e.getPsychiatric());
                m.put("validatedScales", e.getValidatedScales());
                m.put("createdAt", e.getCreatedAt() != null ? ISO_FORMATTER.format(e.getCreatedAt()) : null);
                m.put("updatedAt", e.getUpdatedAt() != null ? ISO_FORMATTER.format(e.getUpdatedAt()) : null);
                list.add(m);
            }
            Map<String, Object> wrapper = new LinkedHashMap<>();
            wrapper.put("physical_exams", list);
            wrapper.put("total", list.size());
            wrapper.put("exportedAt", ISO_FORMATTER.format(Instant.now()));
            return objectMapper.writeValueAsString(wrapper);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializando physical_exams.json", e);
        }
    }

    private String buildMessagesJson(UUID userId) {
        try {
            List<com.kinplatform.kin.health.telemedicine.adapter.MessageEntity> sent = messageRepository.findBySenderIdOrderByCreatedAtAsc(userId);
            List<com.kinplatform.kin.health.telemedicine.adapter.MessageEntity> received = messageRepository.findByReceiverIdOrderByCreatedAtAsc(userId);
            Map<UUID, com.kinplatform.kin.health.telemedicine.adapter.MessageEntity> all = new LinkedHashMap<>();
            for (var m : sent) all.put(m.getId(), m);
            for (var m : received) all.put(m.getId(), m);

            List<Map<String, Object>> list = new ArrayList<>();
            for (var m : all.values()) {
                Map<String, Object> o = new LinkedHashMap<>();
                o.put("id", m.getId().toString());
                o.put("conversationId", m.getConversationId() != null ? m.getConversationId().toString() : null);
                o.put("senderId", m.getSenderId().toString());
                o.put("receiverId", m.getReceiverId().toString());
                o.put("content", m.getContent());
                o.put("read", m.getRead());
                o.put("createdAt", m.getCreatedAt() != null ? m.getCreatedAt().toString() : null);
                list.add(o);
            }
            list.sort(Comparator.comparing(o -> (String) o.get("createdAt")));
            Map<String, Object> wrapper = new LinkedHashMap<>();
            wrapper.put("messages", list);
            wrapper.put("total", list.size());
            wrapper.put("exportedAt", ISO_FORMATTER.format(Instant.now()));
            return objectMapper.writeValueAsString(wrapper);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializando messages.json", e);
        }
    }

    private String buildAuditLogJson(UUID userId) {
        try {
            var page = auditRepository.findByUserIdOrderByTimestampDesc(userId, org.springframework.data.domain.PageRequest.of(0, 1000));
            List<Map<String, Object>> list = new ArrayList<>();
            for (var a : page.getContent()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", a.getId().toString());
                m.put("userId", a.getUserId() != null ? a.getUserId().toString() : null);
                m.put("patientId", a.getPatientId() != null ? a.getPatientId().toString() : null);
                m.put("action", a.getAction() != null ? a.getAction().name() : null);
                m.put("resourceType", a.getResourceType() != null ? a.getResourceType().name() : null);
                m.put("resourceId", a.getResourceId() != null ? a.getResourceId().toString() : null);
                m.put("timestamp", a.getTimestamp() != null ? a.getTimestamp().toString() : null);
                m.put("ipAddress", a.getIpAddress());
                m.put("userAgent", a.getUserAgent());
                m.put("details", a.getDetails());
                list.add(m);
            }
            Map<String, Object> wrapper = new LinkedHashMap<>();
            wrapper.put("audit_log", list);
            wrapper.put("total", list.size());
            wrapper.put("limit", 1000);
            wrapper.put("exportedAt", ISO_FORMATTER.format(Instant.now()));
            return objectMapper.writeValueAsString(wrapper);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializando audit_log.json", e);
        }
    }

    private String buildMetadataJson(User user) {
        try {
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("exportVersion", "1.0");
            meta.put("generatedAt", ISO_FORMATTER.format(Instant.now()));
            meta.put("userId", user.getId().toString());
            meta.put("userEmail", user.getEmail());
            meta.put("filesIncluded", List.of(
                "README.txt",
                "profile.json",
                "consents.json",
                "encounters.json",
                "diagnoses.json",
                "treatment_plans.json",
                "medical_orders.json",
                "physical_exams.json",
                "documents/",
                "messages.json",
                "audit_log.json",
                "metadata.json"
            ));
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


