package com.events.infrastructure.adapter.in.rest.controller;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.smallrye.common.annotation.Blocking;


import static com.events.infrastructure.utils.constants.MessageConstants.CURRENT_USER_RETRIEVED;
import static com.events.infrastructure.utils.constants.MessageConstants.LOGIN_SUCCESS;
import static com.events.infrastructure.utils.constants.MessageConstants.LOGOUT_SUCCESS;
import com.events.application.port.in.LogoutPort;
import static com.events.infrastructure.utils.constants.MessageConstants.REGISTER_SUCCESS;

import com.events.application.port.in.GetCurrentUserPort;
import com.events.application.port.in.LoginPort;
import com.events.application.port.in.UsuariosPort;
import com.events.infrastructure.adapter.in.rest.dto.UpdatePerfilRequest;
import com.events.application.port.in.RegisterPort;
import com.events.infrastructure.adapter.in.rest.dto.ApiResponse;
import com.events.infrastructure.adapter.in.rest.dto.AuthResponse;
import com.events.infrastructure.adapter.in.rest.dto.LoginRequest;
import com.events.infrastructure.adapter.in.rest.dto.RegisterRequest;
import com.events.infrastructure.adapter.in.rest.dto.UsuarioResponse;
import com.events.infrastructure.adapter.in.rest.mapper.UsuarioRestMapper;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirements;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import jakarta.validation.Valid;

@ApplicationScoped
@Blocking
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/api/auth")
@Tag(name = "Autenticacion", description = "Registro, inicio de sesion y usuario actual (US-11)")
public class AuthController {
    @Inject
    public AuthController(RegisterPort registerUseCase, LoginPort loginUseCase, GetCurrentUserPort getCurrentUserUseCase, UsuarioRestMapper mapper, UsuariosPort usuarios, LogoutPort logoutUseCase) {
        this.registerUseCase = registerUseCase;
        this.loginUseCase = loginUseCase;
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.mapper = mapper;
        this.usuarios = usuarios;
        this.logoutUseCase = logoutUseCase;
    }


    private final RegisterPort registerUseCase;
    private final LoginPort loginUseCase;
    private final GetCurrentUserPort getCurrentUserUseCase;
    private final UsuarioRestMapper mapper;
    private final UsuariosPort usuarios;
    private final LogoutPort logoutUseCase;

    @POST
    @Path("/register")
    @SecurityRequirements
    @Operation(summary = "Registrarse", description = "Crea un organizador con rol ORGANIZADOR y devuelve su token de acceso.")
    public Response register(@Valid RegisterRequest request) {
        var result = registerUseCase.execute(request.nombre(), request.correo(), request.password());
        return Response.status(201).entity(ApiResponse.ok(REGISTER_SUCCESS, mapper.toResponse(result))).build();
    }

    @POST
    @Path("/login")
    @SecurityRequirements
    @Operation(summary = "Iniciar sesion", description = "Valida correo y contrasena y devuelve un token de acceso (JWT).")
    public ApiResponse<AuthResponse> login(@Valid LoginRequest request) {
        var result = loginUseCase.execute(request.correo(), request.password());
        return ApiResponse.ok(LOGIN_SUCCESS, mapper.toResponse(result));
    }

    @POST
    @Path("/logout")
    @Consumes(MediaType.WILDCARD)
    @Operation(summary = "Cerrar sesion", description = "Revoca el JWT enviado en Authorization. El cliente debe borrar su token local.")
    public ApiResponse<Void> logout() {
        logoutUseCase.execute();
        return ApiResponse.ok(LOGOUT_SUCCESS, null);
    }

    @GET
    @Path("/me")
    @Operation(summary = "Usuario actual", description = "Devuelve el usuario dueno del token enviado.")
    public ApiResponse<UsuarioResponse> me() {
        return ApiResponse.ok(CURRENT_USER_RETRIEVED, mapper.toResponse(getCurrentUserUseCase.execute()));
    }
    @PATCH
    @Path("/me")
    @Operation(summary = "Editar perfil propio", description = "Cambiar password exige passwordActual. No permite roles ni activo.")
    public ApiResponse<UsuarioResponse> updateMe(@Valid UpdatePerfilRequest body) {
        return ApiResponse.ok("Perfil actualizado correctamente.", mapper.toResponse(usuarios.updateCurrent(
                body.nombre(), body.correo(), body.password(), body.passwordActual())));
    }
    @DELETE
    @Consumes(MediaType.WILDCARD)
    @Path("/me")
    @Operation(summary = "Eliminar cuenta propia", description = "409 si tiene datos de negocio o es el ultimo ADMIN habilitado.")
    public ApiResponse<Void> deleteMe() {
        usuarios.deleteCurrent();
        return ApiResponse.ok("Cuenta eliminada correctamente.", null);
    }
}
