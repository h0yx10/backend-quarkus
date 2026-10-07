package com.events.infrastructure.adapter.in.rest.controller;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.smallrye.common.annotation.Blocking;


import com.events.application.port.in.ListUsuariosPort;
import com.events.application.port.in.UsuariosPort;
import com.events.infrastructure.adapter.in.rest.dto.*;
import com.events.infrastructure.adapter.in.rest.mapper.UsuarioRestMapper;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
@Blocking
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/api/admin/users")
@Tag(name = "Administracion", description = "CRUD de usuarios restringido al rol ADMIN")
public class AdminController {
    @Inject
    public AdminController(ListUsuariosPort listUsuariosUseCase, UsuariosPort usuarios, UsuarioRestMapper mapper) {
        this.listUsuariosUseCase = listUsuariosUseCase;
        this.usuarios = usuarios;
        this.mapper = mapper;
    }

    private final ListUsuariosPort listUsuariosUseCase;
    private final UsuariosPort usuarios;
    private final UsuarioRestMapper mapper;
    @GET
    @Operation(summary = "Listar usuarios")
    public ApiResponse<List<UsuarioResponse>> listUsers() {
        return ApiResponse.ok("Usuarios consultados correctamente.", listUsuariosUseCase.execute().stream().map(mapper::toResponse).toList());
    }
    @GET
    @Path("/{id}")
    @Operation(summary = "Consultar usuario")
    public ApiResponse<UsuarioResponse> get(@PathParam("id") UUID id) {
        return ApiResponse.ok("Usuario consultado correctamente.", mapper.toResponse(usuarios.get(id)));
    }
    @POST
    @Operation(summary = "Crear usuario", description = "Roles por defecto: ORGANIZADOR. No emite token.")
    public Response create(@Valid CreateUsuarioRequest body) {
        return Response.status(201).entity(ApiResponse.ok("Usuario creado correctamente.",
                mapper.toResponse(usuarios.create(body.nombre(), body.correo(), body.password(), body.roles())))).build();
    }
    @PATCH
    @Path("/{id}")
    @Operation(summary = "Actualizar usuario", description = "La contrasena propia exige passwordActual; activo requiere perfil.")
    public ApiResponse<UsuarioResponse> update(@PathParam("id") UUID id, @Valid UpdateUsuarioRequest body) {
        return ApiResponse.ok("Usuario actualizado correctamente.", mapper.toResponse(usuarios.update(id,
                body.nombre(), body.correo(), body.password(), body.passwordActual(), body.roles(), body.activo())));
    }
    @DELETE
    @Consumes(MediaType.WILDCARD)
    @Path("/{id}")
    @Operation(summary = "Eliminar usuario", description = "409 si tiene datos de negocio o es el ultimo ADMIN habilitado.")
    public ApiResponse<Void> delete(@PathParam("id") UUID id) {
        usuarios.delete(id);
        return ApiResponse.ok("Usuario eliminado correctamente.", null);
    }
}
