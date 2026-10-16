package com.events.infrastructure.adapter.in.rest.dto;

import static com.events.infrastructure.utils.constants.MessageConstants.HORAS_ESTIMADAS_POSITIVE;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Previsualiza una reprogramacion antes de confirmarla (US-07). "
        + "Los campos omitidos conservan el valor actual de la subtarea.")
public record OverloadCheckRequest(
        @JsonAlias("fechaObjetivo")
        LocalDate targetDate,
        @DecimalMin(value = "0.0", inclusive = false, message = HORAS_ESTIMADAS_POSITIVE)
        @JsonAlias("horasEstimadas")
        BigDecimal estimatedHours
) {
}
