package com.kinplatform.billing.glosa;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.Set;

/**
 * Maquina de estados del workflow de glosas.
 *
 * RECEIVED -> ANALYZING -> APPEALING -> APPEALED
 * Cualquier estado abierto -> ACCEPTED / REJECTED / CONCILIATED / WRITTEN_OFF (terminal).
 */
@Component
public class AppealWorkflow {

    private static final Set<Glosa.GlosaStatus> TERMINAL = EnumSet.of(
            Glosa.GlosaStatus.ACCEPTED, Glosa.GlosaStatus.REJECTED,
            Glosa.GlosaStatus.CONCILIATED, Glosa.GlosaStatus.WRITTEN_OFF);

    public Glosa analyze(Glosa glosa) {
        requireStatus(glosa, Glosa.GlosaStatus.RECEIVED);
        glosa.setStatus(Glosa.GlosaStatus.ANALYZING);
        return glosa;
    }

    public Glosa startAppeal(Glosa glosa) {
        requireStatus(glosa, Glosa.GlosaStatus.ANALYZING);
        glosa.setStatus(Glosa.GlosaStatus.APPEALING);
        return glosa;
    }

    public Glosa submitAppeal(Glosa glosa, String arguments, OffsetDateTime now) {
        requireStatus(glosa, Glosa.GlosaStatus.APPEALING);
        glosa.setStatus(Glosa.GlosaStatus.APPEALED);
        glosa.setAppealArguments(arguments);
        glosa.setAppealSubmittedAt(now);
        return glosa;
    }

    public Glosa accept(Glosa glosa, BigDecimal resolvedValue, OffsetDateTime now) {
        requireOpen(glosa);
        glosa.setStatus(Glosa.GlosaStatus.ACCEPTED);
        glosa.setResolvedValueCop(resolvedValue);
        glosa.setResolutionDate(now);
        return glosa;
    }

    public Glosa reject(Glosa glosa, OffsetDateTime now) {
        requireOpen(glosa);
        glosa.setStatus(Glosa.GlosaStatus.REJECTED);
        glosa.setResolutionDate(now);
        return glosa;
    }

    public Glosa conciliate(Glosa glosa, BigDecimal resolvedValue, OffsetDateTime now) {
        requireOpen(glosa);
        glosa.setStatus(Glosa.GlosaStatus.CONCILIATED);
        glosa.setResolvedValueCop(resolvedValue);
        glosa.setResolutionDate(now);
        return glosa;
    }

    public Glosa writeOff(Glosa glosa, OffsetDateTime now) {
        requireOpen(glosa);
        glosa.setStatus(Glosa.GlosaStatus.WRITTEN_OFF);
        glosa.setResolutionDate(now);
        return glosa;
    }

    public boolean isAppealExpired(Glosa glosa, OffsetDateTime now) {
        return glosa.getAppealDeadline() != null && glosa.getAppealDeadline().isBefore(now);
    }

    public boolean isTerminal(Glosa.GlosaStatus status) {
        return TERMINAL.contains(status);
    }

    private void requireStatus(Glosa glosa, Glosa.GlosaStatus expected) {
        if (glosa.getStatus() != expected) {
            throw new IllegalStateException(
                    "Transicion invalida: se esperaba " + expected + " pero estaba " + glosa.getStatus());
        }
    }

    private void requireOpen(Glosa glosa) {
        if (isTerminal(glosa.getStatus())) {
            throw new IllegalStateException("La glosa ya esta resuelta: " + glosa.getStatus());
        }
    }
}
