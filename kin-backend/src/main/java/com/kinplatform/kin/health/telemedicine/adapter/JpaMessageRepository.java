package com.kinplatform.kin.health.telemedicine.adapter;

import com.kinplatform.kin.health.telemedicine.domain.Message;
import com.kinplatform.kin.health.telemedicine.port.MessageRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador JPA del puerto {@link MessageRepository} (ADR-032).
 *
 * <p>Persiste los mensajes con el contenido cifrado en reposo
 * ({@link ContentCipher}) y lo descifra al leer, manteniendo el dominio
 * agnóstico del cifrado.</p>
 */
@Component
public class JpaMessageRepository implements MessageRepository {

    private final MessageJpaRepository repository;
    private final ContentCipher cipher;

    public JpaMessageRepository(MessageJpaRepository repository, ContentCipher cipher) {
        this.repository = repository;
        this.cipher = cipher;
    }

    @Override
    @Transactional
    public Message save(Message message) {
        if (message == null) {
            throw new IllegalArgumentException("message no puede ser null");
        }
        MessageEntity entity = repository.findById(message.id()).orElseGet(MessageEntity::new);
        entity.setId(message.id());
        entity.setSenderId(message.senderId());
        entity.setReceiverId(message.receiverId());
        entity.setConversationId(message.conversationId());
        entity.setContent(cipher.encrypt(message.content()));
        entity.setRead(message.read());
        entity.setCreatedAt(message.createdAt());
        MessageEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Message> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> findByConversationId(UUID conversationId) {
        if (conversationId == null) {
            return List.of();
        }
        return repository.findByConversationIdOrderByCreatedAtAsc(conversationId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> findBySenderId(UUID userId) {
        if (userId == null) {
            return List.of();
        }
        return repository.findBySenderIdOrderByCreatedAtAsc(userId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> findByReceiverId(UUID userId) {
        if (userId == null) {
            return List.of();
        }
        return repository.findByReceiverIdOrderByCreatedAtAsc(userId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnreadByReceiver(UUID userId) {
        if (userId == null) {
            return 0;
        }
        return repository.countByReceiverIdAndReadFalse(userId);
    }

    private Message toDomain(MessageEntity entity) {
        if (entity == null) {
            return null;
        }
        return Message.of(
                entity.getId(),
                entity.getSenderId(),
                entity.getReceiverId(),
                entity.getConversationId(),
                cipher.decrypt(entity.getContent()),
                Boolean.TRUE.equals(entity.getRead()),
                entity.getCreatedAt());
    }
}
