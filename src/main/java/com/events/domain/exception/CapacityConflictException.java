package com.events.domain.exception;

import java.math.BigDecimal;

/**
 * Se lanza cuando reprogramar/estimar horas de una subtarea haria que la suma de horas
 * planificadas para un dia supere el limite diario del organizador (US-07).
 */
public class CapacityConflictException extends RuntimeException {

    private final BigDecimal plannedHours;
    private final BigDecimal limitHours;
    private final BigDecimal exceedsBy;

    public CapacityConflictException(BigDecimal plannedHours, BigDecimal limitHours) {
        super("Quedarias con " + plannedHours + "h planificadas (limite " + limitHours + "h)");
        this.plannedHours = plannedHours;
        this.limitHours = limitHours;
        this.exceedsBy = plannedHours.subtract(limitHours);
    }

    public BigDecimal getPlannedHours() {
        return plannedHours;
    }

    public BigDecimal getLimitHours() {
        return limitHours;
    }

    public BigDecimal getExceedsBy() {
        return exceedsBy;
    }
}
