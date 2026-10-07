package com.events.infrastructure.adapter.in.rest.controller;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.smallrye.common.annotation.Blocking;


import static com.events.infrastructure.utils.constants.MessageConstants.OVERLOAD_CHECK_RETRIEVED;

import com.events.application.port.in.CheckOverloadConflictPort;
import com.events.infrastructure.adapter.in.rest.dto.ApiResponse;
import com.events.infrastructure.adapter.in.rest.dto.OverloadCheckRequest;
import com.events.infrastructure.adapter.in.rest.dto.OverloadCheckResponse;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import java.util.UUID;

@ApplicationScoped
@Blocking
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Conflictos", description = "Deteccion de sobrecarga diaria antes de confirmar una reprogramacion (US-07, US-08)")
@Path("/api/subtasks/{id}/conflicts/overload")
public class ConflictController {
    @Inject
    public ConflictController(CheckOverloadConflictPort checkOverloadConflictUseCase) {
        this.checkOverloadConflictUseCase = checkOverloadConflictUseCase;
    }


    private final CheckOverloadConflictPort checkOverloadConflictUseCase;

    @POST
    @Operation(
            summary = "Previsualizar sobrecarga",
            description = "Calcula si reprogramar la subtarea a la fecha/horas indicadas superaria el limite diario, sin guardar el cambio."
    )
    public ApiResponse<OverloadCheckResponse> check(@PathParam("id") UUID id, OverloadCheckRequest request) {
        var result = checkOverloadConflictUseCase.execute(id, request.targetDate(), request.estimatedHours());
        return ApiResponse.ok(OVERLOAD_CHECK_RETRIEVED,
                new OverloadCheckResponse(result.conflict(), result.plannedHours(), result.limitHours(), result.exceedsBy()));
    }
}
