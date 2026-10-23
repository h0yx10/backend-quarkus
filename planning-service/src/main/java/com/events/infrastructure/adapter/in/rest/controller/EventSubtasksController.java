package com.events.infrastructure.adapter.in.rest.controller;
import com.events.infrastructure.adapter.in.rest.dto.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import io.smallrye.common.annotation.Blocking;
import java.util.*;
import org.eclipse.microprofile.openapi.annotations.Operation;
/** Separacion necesaria para resolver las rutas anidadas sin competir con /api/events. */
@Path("/api/events/{eventId}/subtasks") @ApplicationScoped @Blocking
@Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
public class EventSubtasksController {
    @Inject SubtareaController controller;
    @POST @Operation(summary="Agregar subtarea")
    public Response create(@PathParam("eventId") UUID id,@Valid CreateSubtareaRequest request) { return controller.create(id,request); }
    @GET @Operation(summary="Listar subtareas de un evento")
    public ApiResponse<List<SubtareaResponse>> list(@PathParam("eventId") UUID id) { return controller.listByEvento(id); }
}
