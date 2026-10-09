package com.events.application.usecase;

import com.events.application.port.in.CreateSubtareaPort;
import com.events.application.port.out.CurrentOrganizadorPort;
import com.events.application.port.out.CapacidadDiariaRepositoryPort;
import com.events.application.port.out.EventoRepositoryPort;
import com.events.application.port.out.SubtareaRepositoryPort;
import com.events.domain.entity.CapacidadDiaria;
import com.events.domain.entity.Evento;
import com.events.domain.entity.Subtarea;
import com.events.domain.exception.CapacityConflictException;
import com.events.domain.exception.EventoNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class CreateSubtareaUseCase implements CreateSubtareaPort {

    private final EventoRepositoryPort eventoRepository;
    private final SubtareaRepositoryPort subtareaRepository;
    private final CapacidadDiariaRepositoryPort capacidadDiariaRepository;
    private final CurrentOrganizadorPort currentOrganizador;

    public CreateSubtareaUseCase(EventoRepositoryPort eventoRepository, SubtareaRepositoryPort subtareaRepository,
                                 CapacidadDiariaRepositoryPort capacidadDiariaRepository,
                                 CurrentOrganizadorPort currentOrganizador) {
        this.eventoRepository = eventoRepository;
        this.subtareaRepository = subtareaRepository;
        this.capacidadDiariaRepository = capacidadDiariaRepository;
        this.currentOrganizador = currentOrganizador;
    }

    @Override
    public Subtarea execute(UUID eventoId, String nombre, String descripcion, LocalDate fechaObjetivo, BigDecimal horasEstimadas) {
        Evento evento = eventoRepository.findByIdAndOrganizadorId(eventoId, currentOrganizador.currentOrganizadorId())
                .orElseThrow(() -> new EventoNotFoundException("No encontramos el evento solicitado."));
        UUID organizadorId = evento.getOrganizador().getId();
        BigDecimal limite = capacidadDiariaRepository.findCurrentByOrganizadorId(organizadorId)
                .map(CapacidadDiaria::getLimiteHoras)
                .orElse(CapacidadDiaria.LIMITE_POR_DEFECTO);
        BigDecimal otrasHoras = subtareaRepository.sumHorasPlanificadas(organizadorId, fechaObjetivo, null);
        BigDecimal totalPlanificado = otrasHoras.add(horasEstimadas);
        if (totalPlanificado.compareTo(limite) > 0) {
            throw new CapacityConflictException(totalPlanificado, limite);
        }

        Subtarea subtarea = new Subtarea(nombre, descripcion, fechaObjetivo, horasEstimadas);
        subtarea.asociarEvento(evento);
        return subtareaRepository.save(subtarea);
    }
}
