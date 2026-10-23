package com.events.application.usecase;

import com.events.application.port.in.ListEventosPort;
import com.events.application.port.out.CurrentOrganizadorPort;
import com.events.application.port.out.EventoRepositoryPort;
import com.events.domain.entity.Evento;
import java.util.List;

public class ListEventosUseCase implements ListEventosPort {

    private final EventoRepositoryPort eventoRepository;
    private final CurrentOrganizadorPort currentOrganizador;

    public ListEventosUseCase(EventoRepositoryPort eventoRepository, CurrentOrganizadorPort currentOrganizador) {
        this.eventoRepository = eventoRepository;
        this.currentOrganizador = currentOrganizador;
    }

    @Override
    public List<Evento> execute() {
        return eventoRepository.findByOrganizadorId(currentOrganizador.currentOrganizadorId());
    }
}
