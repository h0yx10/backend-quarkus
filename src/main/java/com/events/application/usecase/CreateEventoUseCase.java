package com.events.application.usecase;

import com.events.application.port.in.CreateEventoPort;
import com.events.application.port.in.NuevaSubtareaData;
import com.events.application.port.out.CapacidadDiariaRepositoryPort;
import com.events.application.port.out.CurrentOrganizadorPort;
import com.events.application.port.out.EventoRepositoryPort;
import com.events.application.port.out.OrganizadorRepositoryPort;
import com.events.application.port.out.SubtareaRepositoryPort;
import com.events.domain.entity.Evento;
import com.events.domain.entity.Organizador;
import com.events.domain.entity.Subtarea;
import com.events.domain.exception.OrganizadorNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateEventoUseCase implements CreateEventoPort {

    private final EventoRepositoryPort eventoRepository;
    private final OrganizadorRepositoryPort organizadorRepository;
    private final CapacidadDiariaValidator capacidadDiaria;
    private final CurrentOrganizadorPort currentOrganizador;

    public CreateEventoUseCase(EventoRepositoryPort eventoRepository,
                                OrganizadorRepositoryPort organizadorRepository,
                                CapacidadDiariaRepositoryPort capacidadDiariaRepository,
                                SubtareaRepositoryPort subtareaRepository,
                                CurrentOrganizadorPort currentOrganizador) {
        this.eventoRepository = eventoRepository;
        this.organizadorRepository = organizadorRepository;
        this.capacidadDiaria = new CapacidadDiariaValidator(capacidadDiariaRepository, subtareaRepository);
        this.currentOrganizador = currentOrganizador;
    }

    @Override
    public Evento execute(String nombre, String tipo, String cliente, String contactoCliente,
                           LocalDateTime fechaHora, String lugar, LocalDateTime plazoLimite,
                           List<NuevaSubtareaData> subtareasIniciales) {
        Organizador organizador = organizadorRepository.findById(currentOrganizador.currentOrganizadorId())
                .orElseThrow(() -> new OrganizadorNotFoundException("No encontramos el usuario autenticado."));

        Evento evento = new Evento(nombre, tipo, cliente, contactoCliente, fechaHora, lugar, plazoLimite, organizador);
        List<NuevaSubtareaData> subtareas = subtareasIniciales == null ? List.of() : subtareasIniciales;

        Map<LocalDate, BigDecimal> nuevasHorasPorFecha = new HashMap<>();
        for (NuevaSubtareaData data : subtareas) {
            nuevasHorasPorFecha.merge(data.fechaObjetivo(), data.horasEstimadas(), BigDecimal::add);
        }
        capacidadDiaria.validar(organizador.getId(), nuevasHorasPorFecha, null);

        for (NuevaSubtareaData data : subtareas) {
            evento.agregarSubtarea(new Subtarea(data.nombre(), data.descripcion(), data.fechaObjetivo(), data.horasEstimadas()));
        }

        return eventoRepository.save(evento);
    }
}
