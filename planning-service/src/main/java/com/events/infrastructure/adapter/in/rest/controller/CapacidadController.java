package com.events.infrastructure.adapter.in.rest.controller;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.smallrye.common.annotation.Blocking;


import static com.events.infrastructure.utils.constants.MessageConstants.CAPACIDAD_RETRIEVED;
import static com.events.infrastructure.utils.constants.MessageConstants.CAPACIDAD_UPDATED;

import com.events.application.port.in.GetCapacidadPort;
import com.events.application.port.in.UpdateCapacidadPort;
import com.events.domain.entity.CapacidadDiaria;
import com.events.infrastructure.adapter.in.rest.dto.ApiResponse;
import com.events.infrastructure.adapter.in.rest.dto.CapacidadRequest;
import com.events.infrastructure.adapter.in.rest.dto.CapacidadResponse;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import jakarta.validation.Valid;

@ApplicationScoped
@Blocking
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/api/capacity")
@Tag(name = "Capacidad", description = "Configuracion del limite diario de horas del organizador (US-12)")
public class CapacidadController {
    @Inject
    public CapacidadController(GetCapacidadPort getCapacidadUseCase, UpdateCapacidadPort updateCapacidadUseCase) {
        this.getCapacidadUseCase = getCapacidadUseCase;
        this.updateCapacidadUseCase = updateCapacidadUseCase;
    }


    private final GetCapacidadPort getCapacidadUseCase;
    private final UpdateCapacidadPort updateCapacidadUseCase;

    @GET
    @Operation(summary = "Consultar limite diario", description = "Devuelve el limite configurado, o 6h por defecto si no existe.")
    public ApiResponse<CapacidadResponse> get() {
        CapacidadDiaria capacidad = getCapacidadUseCase.execute();
        return ApiResponse.ok(CAPACIDAD_RETRIEVED, toResponse(capacidad));
    }

    @PUT
    @Operation(summary = "Actualizar limite diario", description = "Actualiza el limite diario (rango permitido: 1..16 horas).")
    public ApiResponse<CapacidadResponse> update(@Valid CapacidadRequest request) {
        CapacidadDiaria capacidad = updateCapacidadUseCase.execute(request.limiteHoras());
        return ApiResponse.ok(CAPACIDAD_UPDATED, toResponse(capacidad));
    }

    private CapacidadResponse toResponse(CapacidadDiaria capacidad) {
        if (capacidad == null) {
            return new CapacidadResponse(CapacidadDiaria.LIMITE_POR_DEFECTO, true, null);
        }
        return new CapacidadResponse(capacidad.getLimiteHoras(), false, capacidad.getFecha());
    }
}
