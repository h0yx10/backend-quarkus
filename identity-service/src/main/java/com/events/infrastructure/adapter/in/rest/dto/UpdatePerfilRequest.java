package com.events.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.*;

public record UpdatePerfilRequest(
        @Size(max = 120) @Pattern(regexp = "(?s).*\\S.*") String nombre,
        @Email @Size(max = 180) @Pattern(regexp = "(?s).*\\S.*") String correo,
        @Size(min = 8) String password,
        String passwordActual
) {
    public UpdatePerfilRequest {
        nombre = nombre == null ? null : nombre.trim();
        correo = correo == null ? null : correo.trim();
    }
}
