package com.events.infrastructure.adapter.in.rest.dto;

import static com.events.infrastructure.utils.constants.MessageConstants.FECHA_HORA_REQUIRED;
import static com.events.infrastructure.utils.constants.MessageConstants.NOMBRE_MAX_LENGTH;
import static com.events.infrastructure.utils.constants.MessageConstants.NOMBRE_REQUIRED;
import static com.events.infrastructure.utils.constants.MessageConstants.TIPO_REQUIRED;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Datos para crear un evento y, opcionalmente, su plan inicial de subtareas")
public record CreateEventoRequest(
        @Schema(example = "Boda Camila y Andres")
        @NotBlank(message = NOMBRE_REQUIRED)
        @Size(max = 180, message = NOMBRE_MAX_LENGTH)
        String nombre,

        @Schema(example = "Social")
        @NotBlank(message = TIPO_REQUIRED)
        String tipo,

        @Schema(example = "Camila Restrepo")
        String cliente,

        @Schema(example = "camila@correo.com")
        String contactoCliente,

        @Schema(example = "2026-05-20T18:00:00")
        @NotNull(message = FECHA_HORA_REQUIRED)
        LocalDateTime fechaHora,

        @Schema(example = "Club Campestre")
        String lugar,

        @Schema(example = "2026-05-15T23:59:00")
        LocalDateTime plazoLimite,

        @Valid
        List<SubtareaInicialRequest> subtareas
) {
}
