package com.events.infrastructure.adapter.in.rest.mapper;

import com.events.domain.entity.Evento;
import com.events.infrastructure.adapter.in.rest.dto.EventoResponse;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class EventoRestMapper {

    private final SubtareaRestMapper subtareaRestMapper;

    public EventoRestMapper(SubtareaRestMapper subtareaRestMapper) {
        this.subtareaRestMapper = subtareaRestMapper;
    }

    public EventoResponse toResponse(Evento evento) {
        return new EventoResponse(
                evento.getId(),
                evento.getNombre(),
                evento.getTipo(),
                evento.getCliente(),
                evento.getContactoCliente(),
                evento.getFechaHora(),
                evento.getLugar(),
                evento.getPlazoLimite(),
                evento.getOrganizador().getId(),
                evento.getSubtareas().stream().map(subtareaRestMapper::toResponse).toList()
        );
    }
}
