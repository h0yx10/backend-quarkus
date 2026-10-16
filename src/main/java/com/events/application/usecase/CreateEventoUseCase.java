package com.events.application.usecase;

import com.events.application.port.in.CreateEventoPort;
import com.events.application.port.in.NuevaSubtareaData;
import com.events.application.port.out.CapacidadDiariaRepositoryPort;
import com.events.application.port.out.CurrentOrganizadorPort;
import com.events.application.port.out.EventoRepositoryPort;
import com.events.application.port.out.OrganizadorRepositoryPort;
import com.events.application.port.out.SubtareaRepositoryPort;
import com.events.domain.entity.CapacidadDiaria;
import com.events.domain.entity.Evento;
import com.events.domain.entity.Organizador;
import com.events.domain.entity.Subtarea;
import com.events.domain.exception.CapacityConflictException;
import java.math.BigDecimal;
import com.events.domain.exception.OrganizadorNotFoundException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CreateEventoUseCase implements CreateEventoPort {

    private final EventoRepositoryPort eventoRepository;
    private final OrganizadorRepositoryPort organizadorRepository;
    private final CapacidadDiariaRepositoryPort capacidadDiariaRepository;
    private final SubtareaRepositoryPort subtareaRepository;
    private final CurrentOrganizadorPort currentOrganizador;

    public CreateEventoUseCase(EventoRepositoryPort eventoRepository,
                                OrganizadorRepositoryPort organizadorRepository,
                                CapacidadDiariaRepositoryPort capacidadDiariaRepository,
                                SubtareaRepositoryPort subtareaRepository,
                                CurrentOrganizadorPort currentOrganizador) {
        this.eventoRepository = eventoRepository;
        this.organizadorRepository = organizadorRepository;
        this.capacidadDiariaRepository = capacidadDiariaRepository;
        this.subtareaRepository = subtareaRepository;
        this.currentOrganizador = currentOrganizador;
    }

    @Override
    public Evento execute(String nombre, String tipo, String cliente, String contactoCliente,
                           LocalDateTime fechaHora, String lugar, LocalDateTime plazoLimite,
                           List<NuevaSubtareaData> subtareasIniciales) {
        Organizador organizador = organizadorRepository.findById(currentOrganizador.currentOrganizadorId())
                .orElseThrow(() -> new OrganizadorNotFoundException("No encontramos el usuario autenticado."));

        validarCapacidad(organizador, subtareasIniciales);
        Evento evento = new Evento(nombre, tipo, cliente, contactoCliente, fechaHora, lugar, plazoLimite, organizador);

        if (subtareasIniciales != null) {
            for (NuevaSubtareaData data : subtareasIniciales) {
                evento.agregarSubtarea(new Subtarea(data.nombre(), data.descripcion(), data.fechaObjetivo(), data.horasEstimadas()));
            }
        }

        return eventoRepository.save(evento);
    }

    private void validarCapacidad(Organizador organizador, List<NuevaSubtareaData> subtareasIniciales) {
        if (subtareasIniciales == null || subtareasIniciales.isEmpty()) {
            return;
        }

        UUID organizadorId = organizador.getId();
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
