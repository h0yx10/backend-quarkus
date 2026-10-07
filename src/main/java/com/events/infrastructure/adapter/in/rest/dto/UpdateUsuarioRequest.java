package com.events.infrastructure.adapter.in.rest.dto;

import com.events.domain.entity.NombreRol;
import jakarta.validation.constraints.*;
import java.util.Set;

public record UpdateUsuarioRequest(
        @Size(max = 120) @Pattern(regexp = "(?s).*\\S.*") String nombre,
        @Email @Size(max = 180) @Pattern(regexp = "(?s).*\\S.*") String correo,
        @Size(min = 8) String password,
        String passwordActual,
        @Size(min = 1) Set<@NotNull NombreRol> roles,
        Boolean activo
) {
    public UpdateUsuarioRequest {
        nombre = nombre == null ? null : nombre.trim();
        correo = correo == null ? null : correo.trim();
    }
}
