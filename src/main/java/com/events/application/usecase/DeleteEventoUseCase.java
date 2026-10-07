package com.events.application.usecase;

import com.events.application.port.in.DeleteEventoPort;
import com.events.application.port.out.CurrentOrganizadorPort;
import com.events.application.port.out.EventoRepositoryPort;
import com.events.domain.exception.EventoNotFoundException;
import java.util.UUID;

public class DeleteEventoUseCase implements DeleteEventoPort {

    private final EventoRepositoryPort eventoRepository;
    private final CurrentOrganizadorPort currentOrganizador;

    public DeleteEventoUseCase(EventoRepositoryPort eventoRepository,
                               CurrentOrganizadorPort currentOrganizador) {
        this.eventoRepository = eventoRepository;
        this.currentOrganizador = currentOrganizador;
    }

    @Override
    public void execute(UUID id) {
        if (!eventoRepository.existsByIdAndOrganizadorId(id, currentOrganizador.currentOrganizadorId())) {
            throw new EventoNotFoundException("No encontramos el evento solicitado.");
        }
        eventoRepository.deleteById(id);
    }
}
