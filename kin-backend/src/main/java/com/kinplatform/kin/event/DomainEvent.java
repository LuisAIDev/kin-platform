package com.kinplatform.kin.event;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Evento de dominio base. Incluye información de tipo para serialización polimórfica
 * con Jackson ({@code @class} property con FQCN).
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public interface DomainEvent {
    String type();
    Object aggregateId();
}
