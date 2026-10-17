package com.events.application.usecase;

import com.events.application.port.in.EventoProgress;
import com.events.application.port.in.GetEventoProgressPort;
import com.events.application.port.out.CurrentOrganizadorPort;
import com.events.application.port.out.EventoRepositoryPort;
import com.events.application.port.out.SubtareaRepositoryPort;
import com.events.domain.entity.EstadoSubtarea;
import com.events.domain.entity.Subtarea;
import com.events.domain.exception.EventoNotFoundException;
import java.util.List;
import java.util.UUID;

public class GetEventoProgressUseCase implements GetEventoProgressPort {

    private final EventoRepositoryPort eventoRepository;
    private final SubtareaRepositoryPort subtareaRepository;
    private final CurrentOrganizadorPort currentOrganizador;

    public GetEventoProgressUseCase(EventoRepositoryPort eventoRepository, SubtareaRepositoryPort subtareaRepository,
                                    CurrentOrganizadorPort currentOrganizador) {
        this.eventoRepository = eventoRepository;
        this.subtareaRepository = subtareaRepository;
        this.currentOrganizador = currentOrganizador;
    }

    @Override
    public EventoProgress execute(UUID eventoId) {
        if (!eventoRepository.existsByIdAndOrganizadorId(eventoId, currentOrganizador.currentOrganizadorId())) {
            throw new EventoNotFoundException("No encontramos el evento solicitado.");
        }
        List<Subtarea> subtareas = subtareaRepository.findByEventoId(eventoId);
        long total = subtareas.size();
        long done = subtareas.stream().filter(s -> s.getEstado() == EstadoSubtarea.DONE).count();
        // Redondeado a 2 decimales (1/3 -> 33.33) para que la barra no muestre 33.333333333333336.
        double percentage = total == 0 ? 0.0 : Math.round(done * 10000.0 / total) / 100.0;
        return new EventoProgress(done, total, percentage);
    }
}
