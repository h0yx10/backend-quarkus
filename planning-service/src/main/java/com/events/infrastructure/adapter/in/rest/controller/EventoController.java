package com.events.infrastructure.adapter.in.rest.controller;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.smallrye.common.annotation.Blocking;


import static com.events.infrastructure.utils.constants.MessageConstants.EVENTO_CREATED;
import static com.events.infrastructure.utils.constants.MessageConstants.EVENTO_DELETED;
import static com.events.infrastructure.utils.constants.MessageConstants.EVENTO_LIST_RETRIEVED;
import static com.events.infrastructure.utils.constants.MessageConstants.EVENTO_PROGRESS_RETRIEVED;
import static com.events.infrastructure.utils.constants.MessageConstants.EVENTO_RETRIEVED;
import static com.events.infrastructure.utils.constants.MessageConstants.EVENTO_UPDATED;

import com.events.application.port.in.CreateEventoPort;
import com.events.application.port.in.DeleteEventoPort;
import com.events.application.port.in.GetEventoPort;
import com.events.application.port.in.GetEventoProgressPort;
import com.events.application.port.in.ListEventosPort;
import com.events.application.port.in.NuevaSubtareaData;
import com.events.application.port.in.UpdateEventoPort;
import com.events.infrastructure.adapter.in.rest.dto.ApiResponse;
import com.events.infrastructure.adapter.in.rest.dto.CreateEventoRequest;
import com.events.infrastructure.adapter.in.rest.dto.EventoResponse;
import com.events.infrastructure.adapter.in.rest.dto.ProgressResponse;
import com.events.infrastructure.adapter.in.rest.dto.UpdateEventoRequest;
import com.events.infrastructure.adapter.in.rest.mapper.EventoRestMapper;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
@Blocking
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/api/events")
@Tag(name = "Eventos", description = "Gestion de eventos y su plan de trabajo logistico (US-01, US-03)")
public class EventoController {
    @Inject
    public EventoController(CreateEventoPort createEventoUseCase, GetEventoPort getEventoUseCase, ListEventosPort listEventosUseCase, UpdateEventoPort updateEventoUseCase, DeleteEventoPort deleteEventoUseCase, GetEventoProgressPort getEventoProgressUseCase, EventoRestMapper mapper) {
        this.createEventoUseCase = createEventoUseCase;
        this.getEventoUseCase = getEventoUseCase;
        this.listEventosUseCase = listEventosUseCase;
        this.updateEventoUseCase = updateEventoUseCase;
        this.deleteEventoUseCase = deleteEventoUseCase;
        this.getEventoProgressUseCase = getEventoProgressUseCase;
        this.mapper = mapper;
    }


    private final CreateEventoPort createEventoUseCase;
    private final GetEventoPort getEventoUseCase;
    private final ListEventosPort listEventosUseCase;
    private final UpdateEventoPort updateEventoUseCase;
    private final DeleteEventoPort deleteEventoUseCase;
    private final GetEventoProgressPort getEventoProgressUseCase;
    private final EventoRestMapper mapper;

    @GET
    @Operation(summary = "Listar eventos", description = "Lista los eventos del usuario autenticado.")
    public ApiResponse<List<EventoResponse>> list() {
        List<EventoResponse> eventos = listEventosUseCase.execute().stream().map(mapper::toResponse).toList();
        return ApiResponse.ok(EVENTO_LIST_RETRIEVED, eventos);
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Consultar un evento", description = "Consulta un evento con sus subtareas por su identificador.")
    public ApiResponse<EventoResponse> get(@PathParam("id") UUID id) {
        return ApiResponse.ok(EVENTO_RETRIEVED, mapper.toResponse(getEventoUseCase.execute(id)));
    }

    @POST
    @Operation(summary = "Crear un evento", description = "Crea un evento y, opcionalmente, su plan inicial de subtareas (US-01, US-02).")
    public Response create(@Valid CreateEventoRequest request) {
        List<NuevaSubtareaData> subtareas = request.subtareas() == null
                ? List.of()
                : request.subtareas().stream()
                        .map(s -> new NuevaSubtareaData(s.name(), s.description(), s.targetDate(), s.estimatedHours()))
                        .toList();

        var evento = createEventoUseCase.execute(
                request.nombre(),
                request.tipo(),
                request.cliente(),
                request.contactoCliente(),
                request.fechaHora(),
                request.lugar(),
                request.plazoLimite(),
                subtareas
        );
        return Response.status(201).entity(ApiResponse.ok(EVENTO_CREATED, mapper.toResponse(evento))).build();
    }

    @PATCH
    @Path("/{id}")
    @Operation(summary = "Actualizar un evento", description = "Actualiza los campos enviados de un evento existente (US-03).")
    public ApiResponse<EventoResponse> update(@PathParam("id") UUID id, UpdateEventoRequest request) {
        var evento = updateEventoUseCase.execute(
                id,
                request.nombre(),
                request.tipo(),
                request.cliente(),
                request.contactoCliente(),
                request.fechaHora(),
                request.lugar(),
                request.plazoLimite()
        );
        return ApiResponse.ok(EVENTO_UPDATED, mapper.toResponse(evento));
    }

    @DELETE
    @Consumes(MediaType.WILDCARD)
    @Path("/{id}")
    @Operation(summary = "Eliminar un evento", description = "Elimina un evento y sus subtareas asociadas (US-03).")
    public ApiResponse<Void> delete(@PathParam("id") UUID id) {
        deleteEventoUseCase.execute(id);
        return ApiResponse.ok(EVENTO_DELETED, null);
    }

    @GET
    @Path("/{id}/progress")
    @Operation(summary = "Progreso del evento", description = "Devuelve el avance de preparacion del evento (US-10).")
    public ApiResponse<ProgressResponse> progress(@PathParam("id") UUID id) {
        var progress = getEventoProgressUseCase.execute(id);
        return ApiResponse.ok(EVENTO_PROGRESS_RETRIEVED,
                new ProgressResponse(progress.done(), progress.total(), progress.percentage()));
    }
}
