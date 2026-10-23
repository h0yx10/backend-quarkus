package com.events.infrastructure.adapter.in.rest.controller;

import com.events.application.port.in.CheckOrganizadorDataPort;
import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.Operation;

/**
 * Endpoints de servicio a servicio. Traefik no enruta /internal, asi que solo son alcanzables
 * desde la red privada de Docker.
 */
@ApplicationScoped
@Blocking
@Path("/internal/organizadores")
@Produces(MediaType.APPLICATION_JSON)
public class InternalController {
    private final CheckOrganizadorDataPort checkOrganizadorData;

    public InternalController(CheckOrganizadorDataPort checkOrganizadorData) {
        this.checkOrganizadorData = checkOrganizadorData;
    }

    @GET
    @Path("/{id}/datos")
    @Operation(hidden = true)
    public Map<String, Boolean> datos(@PathParam("id") UUID organizadorId) {
        return Map.of("tieneDatos", checkOrganizadorData.execute(organizadorId));
    }
}
