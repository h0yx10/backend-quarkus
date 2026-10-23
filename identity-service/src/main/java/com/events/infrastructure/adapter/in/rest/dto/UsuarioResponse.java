package com.events.infrastructure.adapter.in.rest.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record UsuarioResponse(
        UUID id,
        UUID organizadorId,
        String nombre,
        String correo,
        List<String> roles,
        boolean activo,
        LocalDateTime createdAt
) {
}
