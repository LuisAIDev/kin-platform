package com.kinplatform.kin.health.telemedicine;

import com.kinplatform.kin.health.telemedicine.domain.Appointment;
import com.kinplatform.kin.health.telemedicine.domain.Appointment.AppointmentStatus;
import com.kinplatform.kin.health.telemedicine.domain.Message;
import com.kinplatform.kin.health.telemedicine.port.AppointmentRepository;
import com.kinplatform.kin.health.telemedicine.port.MessageRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementaciones en memoria de los puertos de telemedicina para tests
 * (ADR-032).
 */
public class InMemoryTelemedicineRepositories {

    private final List<Message> messages = new ArrayList<>();
    private final List<Appointment> appointments = new ArrayList<>();

    public MessageRepository messageRepository() {
        return new MessageRepository() {
            @Override
            public Message save(Message message) {
                messages.removeIf(m -> m.id().equals(message.id()));
                messages.add(message);
                return message;
            }

            @Override
            public Optional<Message> findById(UUID id) {
                return messages.stream().filter(m -> m.id().equals(id)).findFirst();
            }

            @Override
            public List<Message> findByConversationId(UUID conversationId) {
                return messages.stream()
                        .filter(m -> m.conversationId().equals(conversationId))
                        .sorted(Comparator.comparing(Message::createdAt))
                        .toList();
            }

            @Override
            public List<Message> findBySenderId(UUID userId) {
                return messages.stream()
                        .filter(m -> m.senderId().equals(userId))
                        .toList();
            }

            @Override
            public List<Message> findByReceiverId(UUID userId) {
                return messages.stream()
                        .filter(m -> m.receiverId().equals(userId))
                        .toList();
            }

            @Override
            public long countUnreadByReceiver(UUID userId) {
                return messages.stream()
                        .filter(m -> m.receiverId().equals(userId) && !m.read())
                        .count();
            }
        };
    }

    public AppointmentRepository appointmentRepository() {
        return new AppointmentRepository() {
            @Override
            public Appointment save(Appointment appointment) {
                appointments.removeIf(a -> a.id().equals(appointment.id()));
                appointments.add(appointment);
                return appointment;
            }

            @Override
            public Optional<Appointment> findById(UUID id) {
                return appointments.stream().filter(a -> a.id().equals(id)).findFirst();
            }

            @Override
            public List<Appointment> findByPatientId(UUID patientId) {
                return appointments.stream()
                        .filter(a -> a.patientId().equals(patientId))
                        .toList();
            }

            @Override
            public List<Appointment> findByPhysicianId(UUID physicianId) {
                return appointments.stream()
                        .filter(a -> a.physicianId().equals(physicianId))
                        .toList();
            }

            @Override
            public List<Appointment> findByStatus(AppointmentStatus status) {
                return appointments.stream().filter(a -> a.status() == status).toList();
            }
        };
    }
}
