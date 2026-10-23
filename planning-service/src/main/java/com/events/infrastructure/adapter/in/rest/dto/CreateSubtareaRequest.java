package com.events.infrastructure.adapter.in.rest.dto;

import static com.events.infrastructure.utils.constants.MessageConstants.FECHA_OBJETIVO_REQUIRED;
import static com.events.infrastructure.utils.constants.MessageConstants.HORAS_ESTIMADAS_POSITIVE;
import static com.events.infrastructure.utils.constants.MessageConstants.HORAS_ESTIMADAS_REQUIRED;
import static com.events.infrastructure.utils.constants.MessageConstants.NOMBRE_REQUIRED;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Datos para crear una subtarea logistica de un evento")
public record CreateSubtareaRequest(
        @Schema(example = "Confirmar catering")
        @NotBlank(message = NOMBRE_REQUIRED)
        @JsonAlias("nombre")
        String name,

        @Schema(description = "Descripcion opcional, hasta 255 caracteres", example = "Confirmar menu y horario con el proveedor")
        @JsonAlias("descripcion")
        @Size(max = 255, message = "La descripcion puede tener maximo 255 caracteres.")
        String description,

        @Schema(example = "2026-05-01")
        @NotNull(message = FECHA_OBJETIVO_REQUIRED)
        @JsonAlias("fechaObjetivo")
        LocalDate targetDate,

        @Schema(example = "4")
        @NotNull(message = HORAS_ESTIMADAS_REQUIRED)
        @DecimalMin(value = "0.0", inclusive = false, message = HORAS_ESTIMADAS_POSITIVE)
        @JsonAlias("horasEstimadas")
        BigDecimal estimatedHours
) {
}
