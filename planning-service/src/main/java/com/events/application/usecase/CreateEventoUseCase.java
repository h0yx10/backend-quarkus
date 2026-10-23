package com.events.application.usecase;

import com.events.application.port.in.CreateEventoPort;
import com.events.application.port.in.NuevaSubtareaData;
import com.events.application.port.out.CapacidadDiariaRepositoryPort;
import com.events.application.port.out.CurrentOrganizadorPort;
import com.events.application.port.out.EventoRepositoryPort;
import com.events.application.port.out.SubtareaRepositoryPort;
import com.events.domain.entity.CapacidadDiaria;
import com.events.domain.entity.Evento;
import com.events.domain.entity.Subtarea;
import com.events.domain.exception.CapacityConflictException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CreateEventoUseCase implements CreateEventoPort {

    private final EventoRepositoryPort eventoRepository;
    private final CapacidadDiariaRepositoryPort capacidadDiariaRepository;
    private final SubtareaRepositoryPort subtareaRepository;
    private final CurrentOrganizadorPort currentOrganizador;

    public CreateEventoUseCase(EventoRepositoryPort eventoRepository,
                                CapacidadDiariaRepositoryPort capacidadDiariaRepository,
                                SubtareaRepositoryPort subtareaRepository,
                                CurrentOrganizadorPort currentOrganizador) {
        this.eventoRepository = eventoRepository;
        this.capacidadDiariaRepository = capacidadDiariaRepository;
        this.subtareaRepository = subtareaRepository;
        this.currentOrganizador = currentOrganizador;
    }

    @Override
    public Evento execute(String nombre, String tipo, String cliente, String contactoCliente,
                           LocalDateTime fechaHora, String lugar, LocalDateTime plazoLimite,
                           List<NuevaSubtareaData> subtareasIniciales) {
        // El organizador ya fue verificado por identity-service en el gateway; aqui solo se usa su id.
        UUID organizadorId = currentOrganizador.currentOrganizadorId();

        validarCapacidad(organizadorId, subtareasIniciales);
        Evento evento = new Evento(nombre, tipo, cliente, contactoCliente, fechaHora, lugar, plazoLimite, organizadorId);

        if (subtareasIniciales != null) {
            for (NuevaSubtareaData data : subtareasIniciales) {
                evento.agregarSubtarea(new Subtarea(data.nombre(), data.descripcion(), data.fechaObjetivo(), data.horasEstimadas()));
            }
        }

        return eventoRepository.save(evento);
    }

    private void validarCapacidad(UUID organizadorId, List<NuevaSubtareaData> subtareasIniciales) {
        if (subtareasIniciales == null || subtareasIniciales.isEmpty()) {
            return;
        }

        BigDecimal limite = capacidadDiariaRepository.findCurrentByOrganizadorId(organizadorId)
                .map(CapacidadDiaria::getLimiteHoras)
                .orElse(CapacidadDiaria.LIMITE_POR_DEFECTO);
        Map<java.time.LocalDate, BigDecimal> nuevasHorasPorFecha = new HashMap<>();
        for (NuevaSubtareaData data : subtareasIniciales) {
            nuevasHorasPorFecha.merge(data.fechaObjetivo(), data.horasEstimadas(), BigDecimal::add);
        }
        for (Map.Entry<java.time.LocalDate, BigDecimal> entry : nuevasHorasPorFecha.entrySet()) {
            BigDecimal totalPlanificado = subtareaRepository.sumHorasPlanificadas(organizadorId, entry.getKey(), null)
                    .add(entry.getValue());
            if (totalPlanificado.compareTo(limite) > 0) {
                throw new CapacityConflictException(totalPlanificado, limite);
            }
        }
    }

}
