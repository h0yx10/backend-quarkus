package com.events.infrastructure.config;

import com.events.application.port.in.ChangeSubtareaStatusPort;
import com.events.application.port.in.CheckOverloadConflictPort;
import com.events.application.port.in.CreateEventoPort;
import com.events.application.port.in.CreateSubtareaPort;
import com.events.application.port.in.DeleteEventoPort;
import com.events.application.port.in.DeleteSubtareaPort;
import com.events.application.port.in.GetCapacidadPort;
import com.events.application.port.in.GetEventoPort;
import com.events.application.port.in.GetEventoProgressPort;
import com.events.application.port.in.GetTodayPort;
import com.events.application.port.in.ListEventosPort;
import com.events.application.port.in.ListSubtareasPort;
import com.events.application.port.in.UpdateCapacidadPort;
import com.events.application.port.in.UpdateEventoPort;
import com.events.application.port.in.UpdateSubtareaPort;
import com.events.application.port.out.CapacidadDiariaRepositoryPort;
import com.events.application.port.out.CurrentOrganizadorPort;
import com.events.application.port.out.EventoRepositoryPort;
import com.events.application.port.out.SubtareaRepositoryPort;
import com.events.application.usecase.ChangeSubtareaStatusUseCase;
import com.events.application.usecase.CheckOverloadConflictUseCase;
import com.events.application.usecase.CreateEventoUseCase;
import com.events.application.usecase.CreateSubtareaUseCase;
import com.events.application.usecase.DeleteEventoUseCase;
import com.events.application.usecase.DeleteSubtareaUseCase;
import com.events.application.usecase.GetCapacidadUseCase;
import com.events.application.usecase.GetEventoProgressUseCase;
import com.events.application.usecase.GetEventoUseCase;
import com.events.application.usecase.GetTodayUseCase;
import com.events.application.usecase.ListEventosUseCase;
import com.events.application.usecase.ListSubtareasUseCase;
import com.events.application.usecase.UpdateCapacidadUseCase;
import com.events.application.usecase.UpdateEventoUseCase;
import com.events.application.usecase.UpdateSubtareaUseCase;
import com.events.application.port.in.CheckOrganizadorDataPort;
import com.events.application.usecase.CheckOrganizadorDataUseCase;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UseCaseConfig {

    @Produces
    @ApplicationScoped
    public CreateEventoPort createEventoPort(EventoRepositoryPort eventoRepository,
                                              CapacidadDiariaRepositoryPort capacidadDiariaRepository,
                                              SubtareaRepositoryPort subtareaRepository,
                                              CurrentOrganizadorPort currentOrganizador) {
        return new CreateEventoUseCase(eventoRepository, capacidadDiariaRepository,
                subtareaRepository, currentOrganizador);
    }

    @Produces
    @ApplicationScoped
    public GetEventoPort getEventoPort(EventoRepositoryPort eventoRepository,
                                       CurrentOrganizadorPort currentOrganizador) {
        return new GetEventoUseCase(eventoRepository, currentOrganizador);
    }

    @Produces
    @ApplicationScoped
    public ListEventosPort listEventosPort(EventoRepositoryPort eventoRepository,
                                            CurrentOrganizadorPort currentOrganizador) {
        return new ListEventosUseCase(eventoRepository, currentOrganizador);
    }

    @Produces
    @ApplicationScoped
    public UpdateEventoPort updateEventoPort(EventoRepositoryPort eventoRepository,
                                             CurrentOrganizadorPort currentOrganizador) {
        return new UpdateEventoUseCase(eventoRepository, currentOrganizador);
    }

    @Produces
    @ApplicationScoped
    public DeleteEventoPort deleteEventoPort(EventoRepositoryPort eventoRepository,
                                             CurrentOrganizadorPort currentOrganizador) {
        return new DeleteEventoUseCase(eventoRepository, currentOrganizador);
    }

    @Produces
    @ApplicationScoped
    public CreateSubtareaPort createSubtareaPort(EventoRepositoryPort eventoRepository,
                                                  SubtareaRepositoryPort subtareaRepository,
                                                  CapacidadDiariaRepositoryPort capacidadDiariaRepository,
                                                  CurrentOrganizadorPort currentOrganizador) {
        return new CreateSubtareaUseCase(eventoRepository, subtareaRepository, capacidadDiariaRepository, currentOrganizador);
    }

    @Produces
    @ApplicationScoped
    public ListSubtareasPort listSubtareasPort(EventoRepositoryPort eventoRepository,
                                                SubtareaRepositoryPort subtareaRepository,
                                                CurrentOrganizadorPort currentOrganizador) {
        return new ListSubtareasUseCase(eventoRepository, subtareaRepository, currentOrganizador);
    }

    @Produces
    @ApplicationScoped
    public UpdateSubtareaPort updateSubtareaPort(SubtareaRepositoryPort subtareaRepository,
                                                  CapacidadDiariaRepositoryPort capacidadDiariaRepository,
                                                  CurrentOrganizadorPort currentOrganizador) {
        return new UpdateSubtareaUseCase(subtareaRepository, capacidadDiariaRepository, currentOrganizador);
    }

    @Produces
    @ApplicationScoped
    public ChangeSubtareaStatusPort changeSubtareaStatusPort(SubtareaRepositoryPort subtareaRepository,
                                                              CurrentOrganizadorPort currentOrganizador) {
        return new ChangeSubtareaStatusUseCase(subtareaRepository, currentOrganizador);
    }

    @Produces
    @ApplicationScoped
    public DeleteSubtareaPort deleteSubtareaPort(SubtareaRepositoryPort subtareaRepository,
                                                 CurrentOrganizadorPort currentOrganizador) {
        return new DeleteSubtareaUseCase(subtareaRepository, currentOrganizador);
    }

    @Produces
    @ApplicationScoped
    public GetTodayPort getTodayPort(SubtareaRepositoryPort subtareaRepository,
                                      CurrentOrganizadorPort currentOrganizador) {
        return new GetTodayUseCase(subtareaRepository, currentOrganizador);
    }

    @Produces
    @ApplicationScoped
    public CheckOverloadConflictPort checkOverloadConflictPort(SubtareaRepositoryPort subtareaRepository,
                                                                 CapacidadDiariaRepositoryPort capacidadDiariaRepository,
                                                                 CurrentOrganizadorPort currentOrganizador) {
        return new CheckOverloadConflictUseCase(subtareaRepository, capacidadDiariaRepository, currentOrganizador);
    }

    @Produces
    @ApplicationScoped
    public GetEventoProgressPort getEventoProgressPort(EventoRepositoryPort eventoRepository,
                                                         SubtareaRepositoryPort subtareaRepository,
                                                         CurrentOrganizadorPort currentOrganizador) {
        return new GetEventoProgressUseCase(eventoRepository, subtareaRepository, currentOrganizador);
    }

    @Produces
    @ApplicationScoped
    public GetCapacidadPort getCapacidadPort(CapacidadDiariaRepositoryPort capacidadDiariaRepository,
                                              CurrentOrganizadorPort currentOrganizador) {
        return new GetCapacidadUseCase(capacidadDiariaRepository, currentOrganizador);
    }

    @Produces
    @ApplicationScoped
    public UpdateCapacidadPort updateCapacidadPort(CapacidadDiariaRepositoryPort capacidadDiariaRepository,
                                                          CurrentOrganizadorPort currentOrganizador) {
        return new UpdateCapacidadUseCase(capacidadDiariaRepository, currentOrganizador);
    }

    @Produces
    @ApplicationScoped
    public CheckOrganizadorDataPort checkOrganizadorDataPort(EventoRepositoryPort eventoRepository,
                                                             CapacidadDiariaRepositoryPort capacidadDiariaRepository) {
        return new CheckOrganizadorDataUseCase(eventoRepository, capacidadDiariaRepository);
    }
}
