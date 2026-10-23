package com.events.infrastructure.adapter.in.rest.mapper;

import com.events.domain.entity.Subtarea;
import com.events.infrastructure.adapter.in.rest.dto.SubtareaResponse;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SubtareaRestMapper {

    public SubtareaResponse toResponse(Subtarea subtarea) {
        return new SubtareaResponse(
                subtarea.getId(),
                subtarea.getEvento().getId(),
                subtarea.getNombre(),
                subtarea.getDescripcion(),
                subtarea.getFechaObjetivo(),
                subtarea.getHorasEstimadas(),
                subtarea.getEstado(),
                subtarea.getNota(),
                subtarea.getDoneAt(),
                subtarea.getCreatedAt()
        );
    }
}
