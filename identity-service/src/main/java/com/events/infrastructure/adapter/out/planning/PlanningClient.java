package com.events.infrastructure.adapter.out.planning;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import java.util.UUID;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/** Endpoint interno de planning-service; solo es alcanzable dentro de la red de Docker. */
@RegisterRestClient(configKey = "planning")
public interface PlanningClient {
    record DatosOrganizador(boolean tieneDatos) { }

    @GET
    @Path("/internal/organizadores/{id}/datos")
    DatosOrganizador datos(@PathParam("id") UUID organizadorId);
}
