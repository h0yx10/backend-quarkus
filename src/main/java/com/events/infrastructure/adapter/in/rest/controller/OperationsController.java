package com.events.infrastructure.adapter.in.rest.controller;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import jakarta.inject.Inject;
import javax.sql.DataSource;
import java.net.URI;
import java.util.Map;
@Path("/")
public class OperationsController {
    @Inject DataSource dataSource;
    @GET @Path("swagger-ui.html")
    public Response swagger() { return Response.temporaryRedirect(URI.create("/swagger-ui/")).build(); }
    @GET @Path("actuator/info") @Produces(MediaType.APPLICATION_JSON)
    public Map<String,Object> info() { return Map.of(); }
    @GET @Path("actuator/health") @Produces(MediaType.APPLICATION_JSON)
    public Response health() {
        try(var c=dataSource.getConnection()) { boolean up=c.isValid(2); return Response.status(up?200:503).entity(Map.of("status",up?"UP":"DOWN")).build(); }
        catch(Exception ex) { return Response.status(503).entity(Map.of("status","DOWN")).build(); }
    }
}
