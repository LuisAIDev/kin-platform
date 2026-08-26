package com.kinplatform.kin.health.pilot;

import com.kinplatform.kin.health.physician.port.ClinicalAlertRepository;
import com.kinplatform.kin.health.physician.port.PhysicianPatientRepository;
import com.kinplatform.kin.health.telemedicine.port.AppointmentRepository;
import com.kinplatform.kin.health.telemedicine.port.MessageRepository;
import com.kinplatform.kin.health.triage.domain.TriageConsultation;
import com.kinplatform.kin.health.triage.port.TriageConsultationRepository;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Métricas de éxito del grupo piloto (fase piloto).
 *
 * <p>Calcula indicadores agregados y <strong>anonimizados</strong> (no incluye
 * correos ni nombres; solo conteos y promedios por médico/paciente id):
 * tasa de finalización del triaje, tiempo medio de respuesta del médico a
 * mensajes y citas, volumen de triajes/mensajes/citas y alertas pendientes.
 * Los ids se exponen tal cual (ya son opacos). Alimenta el informe semanal del
 * piloto vía {@code GET /api/v1/admin/health/pilot/metrics}.</p>
 */
@Service
public class PilotMetricsService {

    private final TriageConsultationRepository consultationRepository;
    private final MessageRepository messageRepository;
    private final AppointmentRepository appointmentRepository;
    private final PhysicianPatientRepository assignmentRepository;
    private final ClinicalAlertRepository alertRepository;

    public PilotMetricsService(
            TriageConsultationRepository consultationRepository,
            MessageRepository messageRepository,
            AppointmentRepository appointmentRepository,
            PhysicianPatientRepository assignmentRepository,
            ClinicalAlertRepository alertRepository) {
        this.consultationRepository = consultationRepository;
        this.messageRepository = messageRepository;
        this.appointmentRepository = appointmentRepository;
        this.assignmentRepository = assignmentRepository;
        this.alertRepository = alertRepository;
    }

    @Transactional(readOnly = true)
    public PilotMetrics report() {
        List<UUID> patients = allPatients();
        int patientsWithTriage = 0;
        int totalTriages = 0;
        long totalMessages = 0;
        long totalAppointments = 0;
        Duration totalResponseTime = Duration.ZERO;
        int responseSamples = 0;
        long pendingAlerts = 0;

        for (UUID patientId : patients) {
            List<TriageConsultation> triages = consultationRepository.findByUserId(patientId);
            if (!triages.isEmpty()) {
                patientsWithTriage++;
                totalTriages += triages.size();
            }
            // tiempo medio de respuesta del médico: para cada mensaje enviado
            // por el paciente, se busca la primera respuesta del médico en la
            // misma conversación (proxy del lapso médico-paciente).
            var incoming = messageRepository.findByReceiverId(patientId);
            var outgoing = messageRepository.findBySenderId(patientId);
            totalMessages += incoming.size() + outgoing.size();
            for (var msg : outgoing) {
                var reply = incoming.stream()
                        .filter(o -> o.conversationId().equals(msg.conversationId())
                                && o.createdAt().isAfter(msg.createdAt()))
                        .map(m -> Duration.between(msg.createdAt(), m.createdAt()))
                        .filter(d -> !d.isNegative())
                        .sorted()
                        .findFirst();
                if (reply.isPresent()) {
                    totalResponseTime = totalResponseTime.plus(reply.get());
                    responseSamples++;
                }
            }
            totalAppointments +=
                    appointmentRepository.findByPatientId(patientId).size();
            for (UUID physicianId : assignmentRepository.findPhysicianIdsByPatient(patientId)) {
                pendingAlerts +=
                        alertRepository.findActiveByPhysician(physicianId).size();
            }
        }

        double completionRate = patients.isEmpty() ? 0.0 : (double) patientsWithTriage / patients.size();
        double avgResponseMinutes =
                responseSamples == 0 ? 0.0 : totalResponseTime.toMinutes() / (double) responseSamples;

        return new PilotMetrics(
                patients.size(),
                patientsWithTriage,
                completionRate,
                totalTriages,
                totalMessages,
                totalAppointments,
                avgResponseMinutes,
                pendingAlerts);
    }

    private List<UUID> allPatients() {
        // Todos los pacientes = unión de los pacientes asignados a médicos.
        return assignmentRepository.findAllPatientIds();
    }

    /**
     * Métricas agregadas y anonimizadas (sin correos ni nombres).
     */
    public record PilotMetrics(
            int totalPatients,
            int patientsWithTriage,
            double triageCompletionRate,
            int totalTriages,
            long totalMessages,
            long totalAppointments,
            double avgPhysicianResponseMinutes,
            long pendingAlerts) {}
}
