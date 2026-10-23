package com.events.infrastructure.adapter.in.rest.dto;

import com.events.domain.entity.NombreRol;
import jakarta.validation.constraints.*;
import java.util.Set;

public record CreateUsuarioRequest(
        @NotBlank @Size(max = 120) String nombre,
        @NotBlank @Email @Size(max = 180) String correo,
        @NotBlank @Size(min = 8) String password,
        @Size(min = 1) Set<@NotNull NombreRol> roles
) {
    public CreateUsuarioRequest {
        nombre = nombre == null ? null : nombre.trim();
        correo = correo == null ? null : correo.trim();
    }
}
