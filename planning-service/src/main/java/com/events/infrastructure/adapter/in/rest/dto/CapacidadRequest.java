package com.events.infrastructure.adapter.in.rest.dto;

import static com.events.infrastructure.utils.constants.MessageConstants.LIMITE_HORAS_REQUIRED;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(description = "Nuevo limite diario de horas del organizador (rango permitido: 1..16)")
public record CapacidadRequest(
        @Schema(example = "4")
        @NotNull(message = LIMITE_HORAS_REQUIRED)
        BigDecimal limiteHoras
) {
}
