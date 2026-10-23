package com.events.application.usecase;

import com.events.application.port.in.GetEventoPort;
import com.events.application.port.out.CurrentOrganizadorPort;
import com.events.application.port.out.EventoRepositoryPort;
import com.events.domain.entity.Evento;
import com.events.domain.exception.EventoNotFoundException;
import java.util.UUID;

public class GetEventoUseCase implements GetEventoPort {

    private final EventoRepositoryPort eventoRepository;
    private final CurrentOrganizadorPort currentOrganizador;

    public GetEventoUseCase(EventoRepositoryPort eventoRepository,
                            CurrentOrganizadorPort currentOrganizador) {
        this.eventoRepository = eventoRepository;
        this.currentOrganizador = currentOrganizador;
    }

    @Override
    public Evento execute(UUID id) {
        return eventoRepository.findByIdAndOrganizadorId(id, currentOrganizador.currentOrganizadorId())
                .orElseThrow(() -> new EventoNotFoundException("No encontramos el evento solicitado."));
    }
}
