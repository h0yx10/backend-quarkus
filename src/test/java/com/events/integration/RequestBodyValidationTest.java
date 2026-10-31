package com.events.integration;

import com.events.infrastructure.utils.constants.MessageConstants;
import com.events.support.HttpTestClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static com.events.support.HttpTestClient.*;

/** Flujo HTTP completo sobre H2: bodies vacios y registro de ejecucion con progreso (US-09, US-10). */
@io.quarkus.test.junit.QuarkusTest
class RequestBodyValidationTest {
    HttpTestClient mvc = new HttpTestClient();
    @jakarta.inject.Inject ObjectMapper json;
    String auth;
    String eventId;

    private JsonNode data(HttpTestClient.Actions actions) throws Exception {
        return json.readTree(actions.andReturn().getResponse().body().asString()).get("data");
    }

    @BeforeEach
    void cuentaYEvento() throws Exception {
        var cuenta = data(mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("nombre", "Camila", "correo", "body" + System.nanoTime() + "@correo.com", "password", "Secreta123")))));
        auth = "Bearer " + cuenta.get("accessToken").asText();
        eventId = data(mvc.perform(post("/api/events").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Boda\",\"tipo\":\"Social\",\"fechaHora\":\"2026-12-01T12:00:00\"}"))).get("id").asText();
    }

    private String crearSubtarea(String nombre) throws Exception {
        return data(mvc.perform(post("/api/events/" + eventId + "/subtasks").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + nombre + "\",\"targetDate\":\"2026-11-01\",\"estimatedHours\":1}"))).get("id").asText();
    }

    private void esperaBadRequest(HttpTestClient.Request request) {
        mvc.perform(request.contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(MessageConstants.INVALID_REQUEST));
    }

    @Test
    void unBodyVacioDevuelve400EnVezDeError500() throws Exception {
        String subtareaId = crearSubtarea("Salon");
        esperaBadRequest(post("/api/auth/register"));
        esperaBadRequest(post("/api/auth/login"));
        esperaBadRequest(patch("/api/auth/me").header("Authorization", auth));
        esperaBadRequest(post("/api/events").header("Authorization", auth));
        esperaBadRequest(patch("/api/events/" + eventId).header("Authorization", auth));
        esperaBadRequest(post("/api/events/" + eventId + "/subtasks").header("Authorization", auth));
        esperaBadRequest(patch("/api/subtasks/" + subtareaId).header("Authorization", auth));
        esperaBadRequest(patch("/api/subtasks/" + subtareaId + "/status").header("Authorization", auth));
        esperaBadRequest(post("/api/subtasks/" + subtareaId + "/conflicts/overload").header("Authorization", auth));
        esperaBadRequest(put("/api/capacity").header("Authorization", auth));
    }

    @Test
    void registrarEjecucionGuardaLaNotaYActualizaElProgreso() throws Exception {
        String hecha = crearSubtarea("Salon");
        crearSubtarea("Catering");
        crearSubtarea("Musica");
        String status = "/api/subtasks/" + hecha + "/status";

        var primera = data(mvc.perform(patch(status).header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"DONE\",\"note\":\"Proveedor confirmo\"}")).andExpect(status().isOk()));
        org.assertj.core.api.Assertions.assertThat(primera.get("note").asText()).isEqualTo("Proveedor confirmo");

        var repetida = data(mvc.perform(patch(status).header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"DONE\"}")).andExpect(status().isOk()));
        org.assertj.core.api.Assertions.assertThat(repetida.get("doneAt").asText()).isEqualTo(primera.get("doneAt").asText());
        org.assertj.core.api.Assertions.assertThat(repetida.get("note").asText()).isEqualTo("Proveedor confirmo");

        // Jackson y no jsonPath: REST Assured lee los decimales como float (33.33 -> 33.33000183...).
        var progreso = data(mvc.perform(get("/api/events/" + eventId + "/progress").header("Authorization", auth))
                .andExpect(status().isOk()));
        org.assertj.core.api.Assertions.assertThat(progreso.get("done").asLong()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(progreso.get("total").asLong()).isEqualTo(3);
        org.assertj.core.api.Assertions.assertThat(progreso.get("percentage").asDouble()).isEqualTo(33.33);
    }

    @Test
    void unaSubtareaInicialInvalidaRechazaElEventoCompleto() throws Exception {
        mvc.perform(post("/api/events").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Feria\",\"tipo\":\"Social\",\"fechaHora\":\"2026-12-01T12:00:00\","
                                + "\"subtareas\":[{\"name\":\"\",\"targetDate\":\"2026-11-20\",\"estimatedHours\":0}]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/events").header("Authorization", auth))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void unaNotaDeMasDeMilCaracteresDevuelve400() throws Exception {
        String subtareaId = crearSubtarea("Salon");
        mvc.perform(patch("/api/subtasks/" + subtareaId + "/status").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"POSTPONED\",\"note\":\"" + "a".repeat(1001) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La nota puede tener maximo 1000 caracteres."));
    }
}
