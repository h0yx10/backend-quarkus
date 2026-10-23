package com.events.application.usecase;

import com.events.application.port.in.ListSubtareasPort;
import com.events.application.port.out.CurrentOrganizadorPort;
import com.events.application.port.out.EventoRepositoryPort;
import com.events.application.port.out.SubtareaRepositoryPort;
import com.events.domain.entity.Subtarea;
import com.events.domain.exception.EventoNotFoundException;
import java.util.List;
import java.util.UUID;

public class ListSubtareasUseCase implements ListSubtareasPort {

    private final EventoRepositoryPort eventoRepository;
    private final SubtareaRepositoryPort subtareaRepository;
    private final CurrentOrganizadorPort currentOrganizador;

    public ListSubtareasUseCase(EventoRepositoryPort eventoRepository, SubtareaRepositoryPort subtareaRepository,
                                CurrentOrganizadorPort currentOrganizador) {
        this.eventoRepository = eventoRepository;
        this.subtareaRepository = subtareaRepository;
        this.currentOrganizador = currentOrganizador;
    }

    @Override
    public List<Subtarea> execute(UUID eventoId) {
        if (!eventoRepository.existsByIdAndOrganizadorId(eventoId, currentOrganizador.currentOrganizadorId())) {
            throw new EventoNotFoundException("No encontramos el evento solicitado.");
        }
        return subtareaRepository.findByEventoId(eventoId);
    }
}
