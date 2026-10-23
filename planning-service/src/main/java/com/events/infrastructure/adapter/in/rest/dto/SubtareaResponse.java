package com.events.infrastructure.adapter.in.rest.dto;

import com.events.domain.entity.EstadoSubtarea;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record SubtareaResponse(
        UUID id,
        UUID eventId,
        String name,
        String description,
        LocalDate targetDate,
        BigDecimal estimatedHours,
        EstadoSubtarea status,
        String note,
        LocalDateTime doneAt,
        LocalDateTime createdAt
) {
}
