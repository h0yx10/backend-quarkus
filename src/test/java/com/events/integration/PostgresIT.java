package com.events.integration;
import com.events.support.HttpTestClient;
import com.events.support.EntityIds;
import com.events.support.DatabaseTestClient;
import jakarta.ws.rs.core.MediaType;
import static com.events.support.HttpTestClient.*;


import com.events.application.port.in.*;
import com.events.application.port.out.*;
import com.events.domain.entity.*;
import com.events.domain.exception.*;
import com.fasterxml.jackson.databind.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

/** Ejecutar con -Ppostgres-it y TEST_DB_URL apuntando a una BD VACIA events_test_* desechable. */
@io.quarkus.test.junit.QuarkusTest
@io.quarkus.test.junit.TestProfile(com.events.support.PostgresProfile.class)
@io.quarkus.test.common.QuarkusTestResource(value=com.events.support.PostgresSchemaResource.class,restrictToAnnotatedClass=true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PostgresIT {
    HttpTestClient mvc=new HttpTestClient();
    @jakarta.inject.Inject ObjectMapper json;
    @jakarta.inject.Inject javax.sql.DataSource dataSource;
    DatabaseTestClient jdbc;
    @jakarta.inject.Inject RegisterPort register;
    @jakarta.inject.Inject UsuariosPort usuarios;
    @jakarta.inject.Inject UsuarioRepositoryPort usuarioRepository;
    @jakarta.inject.Inject TransactionPort transaction;
    @jakarta.inject.Inject EventoRepositoryPort eventos;
    @jakarta.inject.Inject CapacidadDiariaRepositoryPort capacidades;
    @jakarta.inject.Inject OrganizadorRepositoryPort organizadores;

    @BeforeAll
    void onlyDisposableDatabase() {
        jdbc=new DatabaseTestClient(dataSource);
        assertThat(jdbc.queryForObject("select current_database()", String.class)).startsWith("events_test_");
    }
    @AfterEach
    void cleanup() {
        jdbc.execute("TRUNCATE tokens_revocados, usuarios, usuario_roles, organizadores, eventos, subtareas, capacidades_diarias CASCADE");
    }
    private JsonNode register(String correo) throws Exception {
        return body(mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("nombre", "Camila", "correo", correo, "password", "Secreta123"))))
                .andExpect(status().isCreated()).andReturn()).get("data");
    }
    private JsonNode body(HttpTestClient.Result result) throws Exception { return json.readTree(result.getResponse().body().asString()); }
    private String token(JsonNode cuenta) { return "Bearer " + cuenta.get("accessToken").asText(); }
    private UUID id(JsonNode cuenta) { return UUID.fromString(cuenta.get("usuario").get("id").asText()); }
    private JsonNode admin() throws Exception {
        JsonNode cuenta = register("admin@correo.com");
        jdbc.update("INSERT INTO usuario_roles SELECT ?, id FROM roles WHERE nombre = 'ADMIN'", id(cuenta));
        return cuenta; // El mismo JWT obtiene el permiso nuevo desde BD.
    }
    private HttpTestClient.Actions patchUser(UUID id, String bearer, String content) throws Exception {
        return mvc.perform(patch("/api/admin/users/" + id).header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON).content(content));
    }
    @Test
    void registrationPersistsDistinctIdsAndBcryptAndIsolatesBusinessData() throws Exception {
        var a = register("A@correo.com"); var b = register("b@correo.com");
        UUID userId = id(a); UUID organizerId = UUID.fromString(a.get("usuario").get("organizadorId").asText());
        assertThat(userId).isNotEqualTo(organizerId);
        Usuario stored = usuarioRepository.findById(userId).orElseThrow();
        assertThat(stored.getPasswordHash()).startsWith("$2").isNotEqualTo("Secreta123");
        assertThat(stored.getCorreo()).isEqualTo("a@correo.com");
        assertThat(stored.getCreatedAt()).isNotNull();
        var claims = json.readTree(Base64.getUrlDecoder().decode(a.get("accessToken").asText().split("\\.")[1]));
        assertThat(claims.get("sub").asText()).isEqualTo(userId.toString());
        mvc.perform(get("/api/auth/me").header("Authorization", token(a)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.organizadorId").value(organizerId.toString()));
        var event = body(mvc.perform(post("/api/events").header("Authorization", token(a))
                .contentType(MediaType.APPLICATION_JSON).content("""
                {"nombre":"Boda", "tipo":"Social", "fechaHora":"2026-12-01T12:00:00",
                 "subtareas":[{"nombre":"Salon", "fechaObjetivo":"2026-11-01", "horasEstimadas":2}]}
                """)).andExpect(status().isCreated()).andReturn()).get("data");
        assertThat(event.get("organizadorId").asText()).isEqualTo(organizerId.toString());
        String eventId = event.get("id").asText();
        String taskId = event.get("subtareas").get(0).get("id").asText();
        mvc.perform(get("/api/events/" + eventId).header("Authorization", token(b))).andExpect(status().isNotFound());
        mvc.perform(get("/api/events").header("Authorization", token(b))).andExpect(jsonPath("$.data.length()").value(0));
        mvc.perform(patch("/api/subtasks/" + taskId).header("Authorization", token(b))
                .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Otra\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/capacity").header("Authorization", token(a)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"limiteHoras\":9}")).andExpect(status().isOk());
        mvc.perform(get("/api/capacity").header("Authorization", token(b)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.limiteHoras").value(6));
    }
    @Test
    void adminCrudAndOwnProfileRespectPermissionsAndPasswords() throws Exception {
        var admin = admin(); String auth = token(admin);
        var user = register("user@correo.com");
        mvc.perform(get("/api/admin/users").header("Authorization", token(user))).andExpect(status().isForbidden());
        var created = body(mvc.perform(post("/api/admin/users").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content("""
                {"nombre":"Solo admin", "correo":"otro@correo.com", "password":"Secreta123", "roles":["ADMIN"]}
                """)).andExpect(status().isCreated()).andExpect(jsonPath("$.data.accessToken").doesNotExist())
                .andExpect(jsonPath("$.data.organizadorId").isEmpty()).andReturn()).get("data");
        UUID other = UUID.fromString(created.get("id").asText());
        mvc.perform(get("/api/admin/users/" + other).header("Authorization", auth)).andExpect(status().isOk());
        mvc.perform(get("/api/admin/users").header("Authorization", auth)).andExpect(jsonPath("$.data.length()").value(3));
        patchUser(other, auth, "{\"roles\":[\"ORGANIZADOR\"],\"nombre\":\"Nuevo\",\"password\":\"Restablecida123\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.organizadorId").isNotEmpty());
        mvc.perform(patch("/api/auth/me").header("Authorization", token(user)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"NuevaClave123\",\"passwordActual\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/auth/me").header("Authorization", token(user)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Propio\",\"correo\":\" USER@correo.com \",\"password\":\"NuevaClave123\",\"passwordActual\":\"Secreta123\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.correo").value("user@correo.com"))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"USER@correo.com\",\"password\":\"Secreta123\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"USER@correo.com\",\"password\":\"NuevaClave123\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").header("Authorization", token(user))).andExpect(status().isOk());
        mvc.perform(delete("/api/admin/users/" + other).header("Authorization", auth)).andExpect(status().isOk());
        mvc.perform(delete("/api/auth/me").header("Authorization", token(user))).andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").header("Authorization", token(user))).andExpect(status().isUnauthorized());
        assertThat(jdbc.queryForObject("select count(*) from organizadores", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from usuario_roles", Integer.class)).isEqualTo(2);
    }
    @Test
    void rejectsMassAssignmentInvalidFieldsAndDuplicates() throws Exception {
        var admin = admin(); var user = register("user@correo.com"); String auth = token(admin);
        mvc.perform(patch("/api/auth/me").header("Authorization", token(user)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"roles\":[\"ADMIN\"]}")).andExpect(status().isBadRequest());
        mvc.perform(patch("/api/auth/me").header("Authorization", token(user)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"activo\":false}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"X\",\"correo\":\"x@correo.com\",\"password\":\"Secreta123\",\"roles\":[\"ADMIN\"]}"))
                .andExpect(status().isBadRequest());
        patchUser(id(user), auth, "{\"correo\":\"ADMIN@correo.com\"}").andExpect(status().isConflict());
        patchUser(id(user), auth, "{\"correo\":\"mal\"}").andExpect(status().isBadRequest());
        patchUser(id(user), auth, "{\"nombre\":\"   \"}").andExpect(status().isBadRequest());
        patchUser(id(user), auth, "{\"roles\":[]}").andExpect(status().isBadRequest());
        patchUser(id(user), auth, "{\"roles\":[\"ROOT\"]}").andExpect(status().isBadRequest());
        patchUser(id(user), auth, json.writeValueAsString(Map.of("password", "é".repeat(37))))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/users/" + UUID.randomUUID()).header("Authorization", auth)).andExpect(status().isNotFound());
        mvc.perform(get("/api/admin/users/no-es-uuid").header("Authorization", auth)).andExpect(status().isBadRequest());
        mvc.perform(delete("/api/admin/users/" + id(user))).andExpect(status().isUnauthorized());
    }
    @Test
    void roleRevocationDeactivationAndDeletionApplyToExistingTokens() throws Exception {
        var admin = admin(); var user = register("user@correo.com"); String auth = token(admin);
        patchUser(id(user), auth, "{\"roles\":[\"ADMIN\"]}").andExpect(status().isOk());
        mvc.perform(get("/api/events").header("Authorization", token(user))).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/users").header("Authorization", token(user))).andExpect(status().isOk());
        patchUser(id(user), auth, "{\"roles\":[\"ORGANIZADOR\"]}").andExpect(status().isOk());
        mvc.perform(get("/api/admin/users").header("Authorization", token(user))).andExpect(status().isForbidden());
        patchUser(id(user), auth, "{\"activo\":false}").andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").header("Authorization", token(user))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"user@correo.com\",\"password\":\"Secreta123\"}"))
                .andExpect(status().isUnauthorized());
        patchUser(id(user), auth, "{\"activo\":true}").andExpect(status().isOk());
        mvc.perform(delete("/api/admin/users/" + id(user)).header("Authorization", auth)).andExpect(status().isOk());
        mvc.perform(get("/api/events").header("Authorization", token(user))).andExpect(status().isUnauthorized());
    }
    @Test
    void lastAdminAndBusinessDependenciesBlockDeletionAndRollBackUpdates() throws Exception {
        var admin = admin(); String auth = token(admin);
        patchUser(id(admin), auth, "{\"nombre\":\"No debe persistir\",\"roles\":[\"ORGANIZADOR\"]}")
                .andExpect(status().isConflict());
        assertThat(usuarioRepository.findById(id(admin)).orElseThrow().getNombre()).isEqualTo("Camila");
        patchUser(id(admin), auth, "{\"activo\":false}").andExpect(status().isConflict());
        mvc.perform(delete("/api/auth/me").header("Authorization", auth)).andExpect(status().isConflict());
        var eventUser = register("evento@correo.com"); var capacityUser = register("capacidad@correo.com");
        UUID eventOrganizer = UUID.fromString(eventUser.get("usuario").get("organizadorId").asText());
        UUID capacityOrganizer = UUID.fromString(capacityUser.get("usuario").get("organizadorId").asText());
        transaction.execute(() -> {
            var organizador = organizadores.findById(eventOrganizer).orElseThrow();
            eventos.save(new Evento("Boda", "Social", null, null, LocalDateTime.now(), null, null, organizador));
            capacidades.save(new CapacidadDiaria(organizadores.findById(capacityOrganizer).orElseThrow(), LocalDate.now(), BigDecimal.valueOf(6)));
            return null;
        });
        mvc.perform(delete("/api/auth/me").header("Authorization", token(eventUser))).andExpect(status().isConflict());
        mvc.perform(delete("/api/admin/users/" + id(capacityUser)).header("Authorization", auth)).andExpect(status().isConflict());
        assertThatThrownBy(() -> jdbc.update("DELETE FROM usuarios WHERE id = ?", id(eventUser)))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM usuarios WHERE id = ?", id(capacityUser)))
                .isInstanceOf(RuntimeException.class);
    }
    @Test
    void concurrentRegistrationsCreateExactlyOneAccount() throws Exception {
        try (var pool = Executors.newFixedThreadPool(2)) {
            var start = new CountDownLatch(1);
            Callable<Boolean> work = () -> {
                start.await();
                try { register.execute("Concurrente", "concurrente@correo.com", "Secreta123"); return true; }
                catch (CorreoYaRegistradoException ex) { return false; }
            };
            var a = pool.submit(work); var b = pool.submit(work); start.countDown();
            assertThat(List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS))).containsExactlyInAnyOrder(true, false);
        }
        assertThat(jdbc.queryForObject("select count(*) from usuarios", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from organizadores", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from usuario_roles", Integer.class)).isEqualTo(1);
    }
    @Test
    void concurrentAdminDeletionsLeaveOneAdmin() throws Exception {
        var a = admin(); var b = register("otroadmin@correo.com");
        jdbc.update("INSERT INTO usuario_roles SELECT ?, id FROM roles WHERE nombre = 'ADMIN'", id(b));
        try (var pool = Executors.newFixedThreadPool(2)) {
            var start = new CountDownLatch(1);
            Callable<Boolean> first = () -> removeAdmin(start, id(a));
            Callable<Boolean> second = () -> removeAdmin(start, id(b));
            var one = pool.submit(first); var two = pool.submit(second); start.countDown();
            assertThat(List.of(one.get(20, TimeUnit.SECONDS), two.get(20, TimeUnit.SECONDS))).containsExactlyInAnyOrder(true, false);
        }
        assertThat(usuarioRepository.countActiveAdmins()).isEqualTo(1);
    }
    @Test
    void failedProfileInsertRollsBackEntireRegistration() {
        jdbc.execute("ALTER TABLE organizadores ADD CONSTRAINT test_fail_profile CHECK (activo = false)");
        try {
            assertThatThrownBy(() -> register.execute("Rollback", "rollback@correo.com", "Secreta123"))
                    .isInstanceOf(RuntimeException.class);
            assertThat(jdbc.queryForObject("select count(*) from usuarios", Integer.class)).isZero();
            assertThat(jdbc.queryForObject("select count(*) from organizadores", Integer.class)).isZero();
            assertThat(jdbc.queryForObject("select count(*) from usuario_roles", Integer.class)).isZero();
        } finally {
            jdbc.execute("ALTER TABLE organizadores DROP CONSTRAINT test_fail_profile");
        }
    }
    @Test
    void adminOnlyAccountCanLoginWithoutProfileAndCannotAccessBusinessRoutes() throws Exception {
        var admin = admin();
        mvc.perform(post("/api/admin/users").header("Authorization", token(admin)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Admin\",\"correo\":\"solo@correo.com\",\"password\":\"Secreta123\",\"roles\":[\"ADMIN\"]}"))
                .andExpect(status().isCreated());
        var login = body(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"solo@correo.com\",\"password\":\"Secreta123\"}"))
                .andExpect(status().isOk()).andReturn()).get("data");
        mvc.perform(get("/api/auth/me").header("Authorization", token(login)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.organizadorId").isEmpty())
                .andExpect(jsonPath("$.data.activo").value(true));
        mvc.perform(get("/api/events").header("Authorization", token(login))).andExpect(status().isForbidden());
        patchUser(id(login), token(admin), "{\"activo\":false}").andExpect(status().isBadRequest());
        mvc.perform(post("/api/admin/users").header("Authorization", token(admin)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Predeterminado\",\"correo\":\"default@correo.com\",\"password\":\"Secreta123\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.roles[0]").value("ORGANIZADOR"))
                .andExpect(jsonPath("$.data.organizadorId").isNotEmpty());
    }
    @jakarta.inject.Inject TokenRevocationPort revocations;
    @Test
    void logoutPersistsRevocationAndKeepsOtherSessionsValid() throws Exception {
        var first = register("logout@correo.com");
        var second = body(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"logout@correo.com\",\"password\":\"Secreta123\"}"))
                .andExpect(status().isOk()).andReturn()).get("data");
        assertThat(token(first)).isNotEqualTo(token(second));
        String raw = first.get("accessToken").asText();
        mvc.perform(post("/api/auth/logout").header("Authorization", token(first)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.message").value("Cerraste sesion correctamente."))
                .andExpect(jsonPath("$.data").isEmpty());
        mvc.perform(get("/api/auth/me").header("Authorization", token(first))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/events").header("Authorization", token(first))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/logout").header("Authorization", token(first))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header("Authorization", token(second))).andExpect(status().isOk());
        String stored = jdbc.queryForObject("select token_hash from tokens_revocados", String.class);
        assertThat(stored).hasSize(64).isNotEqualTo(raw);
        try(var connection=dataSource.getConnection();var query=connection.prepareStatement("SELECT count(*) FROM tokens_revocados WHERE token_hash=?")) {
            query.setString(1,stored); try(var rows=query.executeQuery()) { rows.next(); assertThat(rows.getInt(1)).isEqualTo(1); }
        }
        var loginAgain = body(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"logout@correo.com\",\"password\":\"Secreta123\"}"))
                .andExpect(status().isOk()).andReturn()).get("data");
        mvc.perform(get("/api/auth/me").header("Authorization", token(loginAgain))).andExpect(status().isOk());
    }
    @Test
    void concurrentRevocationsAreIdempotentAndCleanupRespectsClockSkew() throws Exception {
        Instant future = Instant.now().plusSeconds(600);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var start = new CountDownLatch(1);
            Callable<Void> work = () -> { start.await(); revocations.revoke("same-token", future); return null; };
            var a = pool.submit(work); var b = pool.submit(work); start.countDown();
            a.get(10, TimeUnit.SECONDS); b.get(10, TimeUnit.SECONDS);
        }
        assertThat(jdbc.queryForObject("select count(*) from tokens_revocados", Integer.class)).isEqualTo(1);
        revocations.revoke("expired", Instant.now().minusSeconds(300));
        revocations.revoke("in-clock-skew", Instant.now().minusSeconds(10));
        assertThat(revocations.isRevoked("expired")).isFalse();
        assertThat(revocations.isRevoked("in-clock-skew")).isTrue();
        assertThat(revocations.isRevoked("same-token")).isTrue();
    }
    private boolean removeAdmin(CountDownLatch start, UUID id) throws Exception {
        start.await();
        try { usuarios.delete(id); return true; }
        catch (UsuarioConflictException ex) { return false; }
    }
}
