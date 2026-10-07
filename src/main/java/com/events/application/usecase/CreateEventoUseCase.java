package com.events.application.usecase;

import com.events.application.port.in.CreateEventoPort;
import com.events.application.port.in.NuevaSubtareaData;
import com.events.application.port.out.CurrentOrganizadorPort;
import com.events.application.port.out.EventoRepositoryPort;
import com.events.application.port.out.OrganizadorRepositoryPort;
import com.events.domain.entity.Evento;
import com.events.domain.entity.Organizador;
import com.events.domain.entity.Subtarea;
import com.events.domain.exception.OrganizadorNotFoundException;
import java.time.LocalDateTime;
import java.util.List;

public class CreateEventoUseCase implements CreateEventoPort {

    private final EventoRepositoryPort eventoRepository;
    private final OrganizadorRepositoryPort organizadorRepository;
    private final CurrentOrganizadorPort currentOrganizador;

    public CreateEventoUseCase(EventoRepositoryPort eventoRepository,
                                OrganizadorRepositoryPort organizadorRepository,
                                CurrentOrganizadorPort currentOrganizador) {
        this.eventoRepository = eventoRepository;
        this.organizadorRepository = organizadorRepository;
        this.currentOrganizador = currentOrganizador;
    }

    @Override
    public Evento execute(String nombre, String tipo, String cliente, String contactoCliente,
                           LocalDateTime fechaHora, String lugar, LocalDateTime plazoLimite,
                           List<NuevaSubtareaData> subtareasIniciales) {
        Organizador organizador = organizadorRepository.findById(currentOrganizador.currentOrganizadorId())
                .orElseThrow(() -> new OrganizadorNotFoundException("No encontramos el usuario autenticado."));

        Evento evento = new Evento(nombre, tipo, cliente, contactoCliente, fechaHora, lugar, plazoLimite, organizador);

        if (subtareasIniciales != null) {
            for (NuevaSubtareaData data : subtareasIniciales) {
                evento.agregarSubtarea(new Subtarea(data.nombre(), data.descripcion(), data.fechaObjetivo(), data.horasEstimadas()));
            }
        }

        return eventoRepository.save(evento);
    }
}
