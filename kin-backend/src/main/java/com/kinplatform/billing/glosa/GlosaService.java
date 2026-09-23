package com.kinplatform.billing.glosa;

import com.kinplatform.billing.contract.EpsContractRepository;
import com.kinplatform.billing.rips.model.RipsRecord;
import com.kinplatform.common.security.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GlosaService {

    public static final Duration APPEAL_WINDOW = Duration.ofDays(15);

    private final GlosaRepository glosaRepository;
    private final EpsContractRepository contractRepository;
    private final GlosaParser glosaParser;
    private final MatchingEngine matchingEngine;
    private final AppealWorkflow appealWorkflow;

    @Transactional
    public List<Glosa> importFromFile(UUID contractId, String content) {
        UUID organizationId = TenantContext.get();
        contractRepository.findByIdAndOrganizationId(contractId, organizationId)
                .orElseThrow(() -> new EntityNotFoundException("Contrato no encontrado: " + contractId));

        List<Glosa> glosas = glosaParser.parse(content);
        OffsetDateTime deadline = OffsetDateTime.now().plus(APPEAL_WINDOW);
        glosas.forEach(glosa -> {
            glosa.setOrganizationId(organizationId);
            glosa.setContractId(contractId);
            glosa.setAppealDeadline(deadline);
        });
        log.info("Importadas {} glosas para contrato {}", glosas.size(), contractId);
        return glosaRepository.saveAll(glosas);
    }

    public Glosa get(UUID id) {
        return load(id);
    }

    public List<Glosa> findAll() {
        return glosaRepository.findByOrganizationId(TenantContext.get());
    }

    public List<Glosa> findByStatus(Glosa.GlosaStatus status) {
        return glosaRepository.findByOrganizationIdAndStatus(TenantContext.get(), status);
    }

    public long countByStatus(Glosa.GlosaStatus status) {
        return glosaRepository.countByOrganizationIdAndStatus(TenantContext.get(), status);
    }

    @Transactional
    public Glosa analyze(UUID id) {
        return glosaRepository.save(appealWorkflow.analyze(load(id)));
    }

    @Transactional
    public Glosa startAppeal(UUID id) {
        return glosaRepository.save(appealWorkflow.startAppeal(load(id)));
    }

    @Transactional
    public Glosa submitAppeal(UUID id, String arguments) {
        return glosaRepository.save(appealWorkflow.submitAppeal(load(id), arguments, OffsetDateTime.now()));
    }

    @Transactional
    public Glosa resolve(UUID id, Glosa.GlosaStatus status, BigDecimal resolvedValue) {
        Glosa glosa = load(id);
        OffsetDateTime now = OffsetDateTime.now();
        Glosa resolved = switch (status) {
            case ACCEPTED -> appealWorkflow.accept(glosa, resolvedValue, now);
            case REJECTED -> appealWorkflow.reject(glosa, now);
            case CONCILIATED -> appealWorkflow.conciliate(glosa, resolvedValue, now);
            case WRITTEN_OFF -> appealWorkflow.writeOff(glosa, now);
            default -> throw new IllegalArgumentException("Estado de resolucion invalido: " + status);
        };
        return glosaRepository.save(resolved);
    }

    @Transactional
    public Glosa assign(UUID id, UUID userId) {
        Glosa glosa = load(id);
        glosa.setAssignedTo(userId);
        return glosaRepository.save(glosa);
    }

    @Transactional
    public Glosa matchToRips(UUID id) {
        Glosa glosa = load(id);
        Optional<RipsRecord> record = matchingEngine.match(glosa);
        record.ifPresent(r -> {
            glosa.setRipsBatchId(r.getBatchId());
            glosa.setRipsRecordId(r.getId());
        });
        return glosaRepository.save(glosa);
    }

    private Glosa load(UUID id) {
        return glosaRepository.findByIdAndOrganizationId(id, TenantContext.get())
                .orElseThrow(() -> new EntityNotFoundException("Glosa no encontrada: " + id));
    }
}
