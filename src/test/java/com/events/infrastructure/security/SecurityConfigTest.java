package com.events.infrastructure.security;
import com.events.support.HttpTestClient;
import com.events.support.EntityIds;
import com.events.support.DatabaseTestClient;
import jakarta.ws.rs.core.MediaType;
import static com.events.support.HttpTestClient.*;


import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.events.application.port.in.AuthResult;
import com.events.application.port.in.GetCurrentUserPort;
import com.events.application.port.in.ListEventosPort;
import com.events.application.port.in.ListUsuariosPort;
import com.events.application.port.in.LoginPort;
import com.events.application.port.in.RegisterPort;
import com.events.application.port.out.CurrentOrganizadorPort;
import com.events.domain.entity.NombreRol;
import com.events.domain.entity.Usuario;
import com.events.domain.entity.Rol;
import com.events.infrastructure.adapter.in.rest.controller.AdminController;
import com.events.infrastructure.adapter.in.rest.controller.AuthController;
import com.events.infrastructure.adapter.in.rest.controller.EventoController;
import com.events.infrastructure.adapter.in.rest.exception.GlobalExceptionHandler;
import com.events.infrastructure.adapter.in.rest.mapper.EventoRestMapper;
import com.events.infrastructure.adapter.in.rest.mapper.SubtareaRestMapper;
import com.events.infrastructure.adapter.in.rest.mapper.UsuarioRestMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Valida las rutas protegidas end-to-end a nivel web: sin token -> 401, token invalido -> 401,
 * rol insuficiente -> 403, token valido -> 200, y rutas publicas accesibles sin token.
 */
@io.quarkus.test.junit.QuarkusTest
class SecurityConfigTest {

        private final HttpTestClient mockMvc=new HttpTestClient();
    @jakarta.inject.Inject
    private JwtTokenProviderAdapter tokenProvider;
    @jakarta.inject.Inject
    private CurrentOrganizadorPort currentOrganizador;

    @io.quarkus.test.InjectMock
    private com.events.application.port.in.LogoutPort logoutPort;
    @io.quarkus.test.InjectMock
    private com.events.application.port.out.TokenRevocationPort revocations;
    @io.quarkus.test.InjectMock
    private RegisterPort registerPort;
    @io.quarkus.test.InjectMock
    private LoginPort loginPort;
    @io.quarkus.test.InjectMock
    private GetCurrentUserPort getCurrentUserPort;
    @io.quarkus.test.InjectMock
    private ListUsuariosPort listUsuariosPort;
    @io.quarkus.test.InjectMock
    private com.events.application.port.in.UsuariosPort usuariosPort;
    @io.quarkus.test.InjectMock
    private com.events.application.port.out.UsuarioRepositoryPort usuarioRepository;
    @io.quarkus.test.InjectMock
    private com.events.application.port.out.OrganizadorRepositoryPort organizadorRepository;
    @io.quarkus.test.InjectMock
    private ListEventosPort listEventosPort;
    @io.quarkus.test.InjectMock
    private com.events.application.port.in.CreateEventoPort createEventoPort;
    @io.quarkus.test.InjectMock
    private com.events.application.port.in.GetEventoPort getEventoPort;
    @io.quarkus.test.InjectMock
    private com.events.application.port.in.UpdateEventoPort updateEventoPort;
    @io.quarkus.test.InjectMock
    private com.events.application.port.in.DeleteEventoPort deleteEventoPort;
    @io.quarkus.test.InjectMock
    private com.events.application.port.in.GetEventoProgressPort getEventoProgressPort;

    private Usuario usuarioCon(NombreRol... roles) {
        Usuario organizador = new Usuario("Camila", "camila@correo.com", "hash");
        EntityIds.setField(organizador, "id", UUID.randomUUID());
        var perfil = organizador.habilitarComoOrganizador();
        for (NombreRol rol : roles) {
            organizador.asignarRol(new Rol(rol));
        }
        return organizador;
    }

    private String bearer(Usuario organizador) {
        when(usuarioRepository.findById(organizador.getId())).thenReturn(java.util.Optional.of(organizador));
        EntityIds.setField(organizador.getOrganizador(), "id", UUID.randomUUID());
        when(organizadorRepository.findByUsuarioId(organizador.getId())).thenReturn(java.util.Optional.of(organizador.getOrganizador()));
        return "Bearer " + tokenProvider.generate(organizador);
    }

    @Test
    void rutaProtegidaSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/events"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(RestAuthenticationErrorHandler.TOKEN_REQUIRED));
    }

    @Test
    void tokenInvalidoDevuelve401() throws Exception {
        mockMvc.perform(get("/api/events").header("Authorization", "Bearer no-es-un-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(RestAuthenticationErrorHandler.TOKEN_INVALID));
    }

    @Test
    void tokenValidoPermiteAccederYExponeElUsuarioActual() throws Exception {
        Usuario usuario = usuarioCon(NombreRol.ORGANIZADOR);
        when(listEventosPort.execute()).thenAnswer(invocation -> {
            // El adaptador resuelve el "sub" del JWT como organizador actual.
            org.assertj.core.api.Assertions.assertThat(currentOrganizador.currentOrganizadorId()).isEqualTo(usuario.getOrganizador().getId());
            return List.of();
        });

        mockMvc.perform(get("/api/events").header("Authorization", bearer(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void rutaAdminConRolOrganizadorDevuelve403() throws Exception {
        mockMvc.perform(get("/api/admin/users").header("Authorization", bearer(usuarioCon(NombreRol.ORGANIZADOR))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(RestAuthenticationErrorHandler.ACCESS_DENIED));
    }

    @Test
    void rutaAdminConRolAdminDevuelve200() throws Exception {
        when(listUsuariosPort.execute()).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/users").header("Authorization", bearer(usuarioCon(NombreRol.ADMIN))))
                .andExpect(status().isOk());
    }

    @Test
    void loginEsPublico() throws Exception {
        Usuario usuario = usuarioCon(NombreRol.ORGANIZADOR);
        when(loginPort.execute(any(), any())).thenReturn(new AuthResult("jwt", 3600, usuario));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"camila@correo.com\",\"password\":\"Secreta123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("jwt"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.usuario.roles[0]").value("ORGANIZADOR"));
    }

    @Test
    void registroValidaElBody() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Camila\",\"correo\":\"no-es-correo\",\"password\":\"Secreta123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("correo")));
    }

    private String signed(String subject, java.time.Instant expires) {
        try {
            var claims=new com.nimbusds.jwt.JWTClaimsSet.Builder().issuer("events-api").subject(subject)
                    .issueTime(java.util.Date.from(expires.minusSeconds(600))).expirationTime(java.util.Date.from(expires)).build();
            var jwt=new com.nimbusds.jwt.SignedJWT(new com.nimbusds.jose.JWSHeader(com.nimbusds.jose.JWSAlgorithm.HS256),claims);
            jwt.sign(new com.nimbusds.jose.crypto.MACSigner("test-secret-test-secret-test-secret-123".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            return "Bearer "+jwt.serialize();
        } catch(Exception ex) { throw new AssertionError(ex); }
    }
    @Test
    void tokenFirmadoConSubjectInvalidoDevuelve401() throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", signed("no-es-uuid", java.time.Instant.now().plusSeconds(600))))
                .andExpect(status().isUnauthorized());
    }
    @Test
    void tokenExpiradoDevuelve401() throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", signed(UUID.randomUUID().toString(), java.time.Instant.now().minusSeconds(120))))
                .andExpect(status().isUnauthorized());
    }
    @Test
    void logoutExigeToken() throws Exception {
        mockMvc.perform(post("/api/auth/logout")).andExpect(status().isUnauthorized());
        org.mockito.Mockito.verifyNoInteractions(logoutPort);
    }
    @Test
    void logoutDevuelveMensajeYDataNull() throws Exception {
        mockMvc.perform(post("/api/auth/logout").header("Authorization", bearer(usuarioCon(NombreRol.ADMIN))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cerraste sesion correctamente."))
                .andExpect(jsonPath("$.data").isEmpty());
        org.mockito.Mockito.verify(logoutPort).execute();
    }
    @Test
    void tokenRevocadoNoAccedeANingunaRutaProtegida() throws Exception {
        String bearer = bearer(usuarioCon(NombreRol.ADMIN));
        when(revocations.isRevoked(bearer.substring(7))).thenReturn(true);
        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/users").header("Authorization", bearer)).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/logout").header("Authorization", bearer)).andExpect(status().isUnauthorized());
        org.mockito.Mockito.verifyNoInteractions(logoutPort);
    }
    @Test
    void aceptaTokenLegacyNimbusSinJtiYConsultaRolesActuales() throws Exception {
        Usuario usuario=usuarioCon(NombreRol.ADMIN);
        bearer(usuario);
        when(listUsuariosPort.execute()).thenReturn(List.of());
        mockMvc.perform(get("/api/admin/users").header("Authorization",
                signed(usuario.getId().toString(), java.time.Instant.now().plusSeconds(600))))
                .andExpect(status().isOk());
    }
    @Test void uuidInvalidoConservaElMensajeDelContrato() throws Exception {
        mockMvc.perform(get("/api/events/no-es-uuid").header("Authorization",bearer(usuarioCon(NombreRol.ORGANIZADOR))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Failed to convert value of type 'java.lang.String' to required type 'java.util.UUID'; Invalid UUID string: no-es-uuid"));
    }
}
