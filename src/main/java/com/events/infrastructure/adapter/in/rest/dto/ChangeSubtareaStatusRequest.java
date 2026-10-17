package com.events.infrastructure.adapter.in.rest.dto;

import static com.events.infrastructure.utils.constants.MessageConstants.ESTADO_REQUIRED;

import com.events.domain.entity.EstadoSubtarea;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Nuevo estado de ejecucion de una subtarea (US-09)")
public record ChangeSubtareaStatusRequest(
        @Schema(example = "DONE")
        @NotNull(message = ESTADO_REQUIRED)
        @JsonAlias("estado")
        EstadoSubtarea status,

        @Schema(example = "Esperando confirmacion de salon",
                description = "Nota opcional para cualquier estado, maximo 1000 caracteres. Omitir o null conserva la anterior; \"\" la vacia.")
        @Size(max = 1000, message = "La nota puede tener maximo 1000 caracteres.")
        @JsonAlias("nota")
        String note
) {
}
