package com.kinplatform.common.usage;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Reserva de presupuesto de IA para una solicitud, antes de llamar al LLM.
 * Se crea con una estimación de costo y se reconcilia con el uso real cuando
 * el proveedor reporta los tokens. La reserva es el mecanismo que impide
 * superar el presupuesto con requests concurrentes.
 */
public record AiReservation(UUID userId, UsagePeriod period, BigDecimal estimatedCostUsd) {}


