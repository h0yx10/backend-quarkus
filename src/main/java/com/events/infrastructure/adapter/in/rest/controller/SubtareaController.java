package com.events.infrastructure.adapter.in.rest.controller;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.smallrye.common.annotation.Blocking;


import static com.events.infrastructure.utils.constants.MessageConstants.SUBTAREA_CREATED;
import static com.events.infrastructure.utils.constants.MessageConstants.SUBTAREA_DELETED;
import static com.events.infrastructure.utils.constants.MessageConstants.SUBTAREA_LIST_RETRIEVED;
import static com.events.infrastructure.utils.constants.MessageConstants.SUBTAREA_STATUS_UPDATED;
import static com.events.infrastructure.utils.constants.MessageConstants.SUBTAREA_UPDATED;

import com.events.application.port.in.ChangeSubtareaStatusPort;
import com.events.application.port.in.CreateSubtareaPort;
import com.events.application.port.in.DeleteSubtareaPort;
import com.events.application.port.in.ListSubtareasPort;
import com.events.application.port.in.UpdateSubtareaPort;
import com.events.infrastructure.adapter.in.rest.dto.ApiResponse;
import com.events.infrastructure.adapter.in.rest.dto.ChangeSubtareaStatusRequest;
import com.events.infrastructure.adapter.in.rest.dto.CreateSubtareaRequest;
import com.events.infrastructure.adapter.in.rest.dto.SubtareaResponse;
import com.events.infrastructure.adapter.in.rest.dto.UpdateSubtareaRequest;
import com.events.infrastructure.adapter.in.rest.mapper.SubtareaRestMapper;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
@Blocking
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Subtareas", description = "Plan de trabajo logistico, reprogramacion y ejecucion (US-02, US-03, US-06, US-09)")
@Path("/api/subtasks")
public class SubtareaController {
    @Inject
    public SubtareaController(CreateSubtareaPort createSubtareaUseCase, ListSubtareasPort listSubtareasUseCase, UpdateSubtareaPort updateSubtareaUseCase, ChangeSubtareaStatusPort changeSubtareaStatusUseCase, DeleteSubtareaPort deleteSubtareaUseCase, SubtareaRestMapper mapper) {
        this.createSubtareaUseCase = createSubtareaUseCase;
        this.listSubtareasUseCase = listSubtareasUseCase;
        this.updateSubtareaUseCase = updateSubtareaUseCase;
        this.changeSubtareaStatusUseCase = changeSubtareaStatusUseCase;
        this.deleteSubtareaUseCase = deleteSubtareaUseCase;
        this.mapper = mapper;
    }


    private final CreateSubtareaPort createSubtareaUseCase;
    private final ListSubtareasPort listSubtareasUseCase;
    private final UpdateSubtareaPort updateSubtareaUseCase;
    private final ChangeSubtareaStatusPort changeSubtareaStatusUseCase;
    private final DeleteSubtareaPort deleteSubtareaUseCase;
    private final SubtareaRestMapper mapper;

    @Operation(summary = "Agregar subtarea", description = "Crea una subtarea logistica para un evento (US-02).")
    public Response create(
            @PathParam("eventId") UUID eventId,
            @Valid CreateSubtareaRequest request
    ) {
        var subtarea = createSubtareaUseCase.execute(eventId, request.name(), request.description(), request.targetDate(), request.estimatedHours());
        return Response.status(201).entity(ApiResponse.ok(SUBTAREA_CREATED, mapper.toResponse(subtarea))).build();
    }

    @Operation(summary = "Listar subtareas de un evento", description = "Lista las subtareas logisticas de un evento.")
    public ApiResponse<List<SubtareaResponse>> listByEvento(@PathParam("eventId") UUID eventId) {
        List<SubtareaResponse> subtareas = listSubtareasUseCase.execute(eventId).stream().map(mapper::toResponse).toList();
        return ApiResponse.ok(SUBTAREA_LIST_RETRIEVED, subtareas);
    }

    @PATCH
    @Path("/{id}")
    @Operation(
            summary = "Editar o reprogramar una subtarea",
            description = "Actualiza los campos enviados. Cambiar targetDate y/o estimatedHours valida "
                    + "sobrecarga diaria (US-06, US-07) y devuelve 409 si se supera el limite."
    )
    public ApiResponse<SubtareaResponse> update(@PathParam("id") UUID id, @Valid UpdateSubtareaRequest request) {
        var subtarea = updateSubtareaUseCase.execute(id, request.name(), request.description(), request.targetDate(), request.estimatedHours());
        return ApiResponse.ok(SUBTAREA_UPDATED, mapper.toResponse(subtarea));
    }

    @PATCH
    @Path("/{id}/status")
    @Operation(summary = "Registrar ejecucion", description = "Marca una subtarea como hecha o pospuesta, con nota opcional (US-09).")
    public ApiResponse<SubtareaResponse> changeStatus(@PathParam("id") UUID id, @Valid ChangeSubtareaStatusRequest request) {
        var subtarea = changeSubtareaStatusUseCase.execute(id, request.status(), request.note());
        return ApiResponse.ok(SUBTAREA_STATUS_UPDATED, mapper.toResponse(subtarea));
    }

    @DELETE
    @Consumes(MediaType.WILDCARD)
    @Path("/{id}")
    @Operation(summary = "Eliminar subtarea", description = "Elimina una subtarea logistica (US-03).")
    public ApiResponse<Void> delete(@PathParam("id") UUID id) {
        deleteSubtareaUseCase.execute(id);
        return ApiResponse.ok(SUBTAREA_DELETED, null);
    }
}
