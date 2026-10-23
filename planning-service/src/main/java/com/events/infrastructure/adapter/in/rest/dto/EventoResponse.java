package com.events.infrastructure.adapter.in.rest.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record EventoResponse(
        UUID id,
        String nombre,
        String tipo,
        String cliente,
        String contactoCliente,
        LocalDateTime fechaHora,
        String lugar,
        LocalDateTime plazoLimite,
        UUID organizadorId,
        List<SubtareaResponse> subtareas
) {
}
