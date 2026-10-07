package com.events.infrastructure.adapter.in.rest.dto;

import static com.events.infrastructure.utils.constants.MessageConstants.CORREO_REQUIRED;
import static com.events.infrastructure.utils.constants.MessageConstants.PASSWORD_REQUIRED;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credenciales para iniciar sesion")
public record LoginRequest(
        @Schema(example = "camila@correo.com")
        @NotBlank(message = CORREO_REQUIRED)
        String correo,

        @Schema(example = "Secreta123")
        @NotBlank(message = PASSWORD_REQUIRED)
        String password
) {
}
