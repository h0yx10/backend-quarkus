package com.events.infrastructure.adapter.in.rest.controller;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.smallrye.common.annotation.Blocking;


import static com.events.infrastructure.utils.constants.MessageConstants.TODAY_RETRIEVED;

import com.events.application.port.in.GetTodayPort;
import com.events.application.port.in.TodayGroups;
import com.events.domain.entity.EstadoSubtarea;
import com.events.infrastructure.adapter.in.rest.dto.ApiResponse;
import com.events.infrastructure.adapter.in.rest.dto.TodayResponse;
import com.events.infrastructure.adapter.in.rest.mapper.SubtareaRestMapper;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import java.util.UUID;

@ApplicationScoped
@Blocking
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Hoy", description = "Vista de gestiones urgentes del dia, con filtros (US-04, US-05)")
@Path("/")
public class TodayController {
    @Inject
    public TodayController(GetTodayPort getTodayUseCase, SubtareaRestMapper mapper) {
        this.getTodayUseCase = getTodayUseCase;
        this.mapper = mapper;
    }


    private final GetTodayPort getTodayUseCase;
    private final SubtareaRestMapper mapper;

    @GET
    @Path("/api/today")
    @Operation(
            summary = "Vista Hoy",
            description = "Subtareas no DONE agrupadas en Vencidas/Para hoy/Proximas, con filtros opcionales por evento y estado."
    )
    public ApiResponse<TodayResponse> today(
            @QueryParam("eventId") UUID eventId,
            @QueryParam("status") EstadoSubtarea status
    ) {
        TodayGroups groups = getTodayUseCase.execute(eventId, status);
        TodayResponse response = new TodayResponse(
                groups.vencidas().stream().map(mapper::toResponse).toList(),
                groups.paraHoy().stream().map(mapper::toResponse).toList(),
                groups.proximas().stream().map(mapper::toResponse).toList(),
                TodayGroups.REGLA
        );
        return ApiResponse.ok(TODAY_RETRIEVED, response);
    }
}
