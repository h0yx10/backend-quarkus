package com.events.application.usecase;

import com.events.application.port.in.CreateSubtareaPort;
import com.events.application.port.out.CurrentOrganizadorPort;
import com.events.application.port.out.EventoRepositoryPort;
import com.events.application.port.out.SubtareaRepositoryPort;
import com.events.domain.entity.Evento;
import com.events.domain.entity.Subtarea;
import com.events.domain.exception.EventoNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class CreateSubtareaUseCase implements CreateSubtareaPort {

    private final EventoRepositoryPort eventoRepository;
    private final SubtareaRepositoryPort subtareaRepository;
    private final CurrentOrganizadorPort currentOrganizador;

    public CreateSubtareaUseCase(EventoRepositoryPort eventoRepository, SubtareaRepositoryPort subtareaRepository,
                                 CurrentOrganizadorPort currentOrganizador) {
        this.eventoRepository = eventoRepository;
        this.subtareaRepository = subtareaRepository;
        this.currentOrganizador = currentOrganizador;
    }

    @Override
    public Subtarea execute(UUID eventoId, String nombre, String descripcion, LocalDate fechaObjetivo, BigDecimal horasEstimadas) {
        Evento evento = eventoRepository.findByIdAndOrganizadorId(eventoId, currentOrganizador.currentOrganizadorId())
                .orElseThrow(() -> new EventoNotFoundException("No encontramos el evento solicitado."));
        Subtarea subtarea = new Subtarea(nombre, descripcion, fechaObjetivo, horasEstimadas);
        subtarea.asociarEvento(evento);
        return subtareaRepository.save(subtarea);
    }
}
