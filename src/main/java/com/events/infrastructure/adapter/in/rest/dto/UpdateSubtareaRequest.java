package com.events.infrastructure.adapter.in.rest.dto;

import static com.events.infrastructure.utils.constants.MessageConstants.HORAS_ESTIMADAS_POSITIVE;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Campos a actualizar de una subtarea; solo se aplican los que se envien. "
        + "Enviar targetDate y/o estimatedHours reprograma la subtarea (US-06) y valida sobrecarga (US-07).")
public record UpdateSubtareaRequest(
        @JsonAlias("nombre")
        String name,
        @JsonAlias("descripcion")
        @Schema(description = "Descripcion opcional, maximo 255 caracteres. Omitir o null conserva la anterior.")
        @Size(max = 255, message = "La descripcion puede tener maximo 255 caracteres.")
        String description,
        @JsonAlias("fechaObjetivo")
        LocalDate targetDate,
        @DecimalMin(value = "0.0", inclusive = false, message = HORAS_ESTIMADAS_POSITIVE)
        @JsonAlias("horasEstimadas")
        BigDecimal estimatedHours
) {
}
