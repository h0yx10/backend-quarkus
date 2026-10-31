package com.events.integration;

import com.events.support.HttpTestClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.MediaType;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static com.events.support.HttpTestClient.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 5 (C4): recorrido completo T1-T4 contra la API, comprobando en cada paso que la vista Hoy,
 * el detalle del evento y el progreso cuentan lo mismo.
 */
@io.quarkus.test.junit.QuarkusTest
class FlujoT1aT4Test {
    HttpTestClient mvc = new HttpTestClient();
    @jakarta.inject.Inject ObjectMapper json;

    private final LocalDate hoy = LocalDate.now();
    private String auth;

    private JsonNode body(HttpTestClient.Actions actions) throws Exception {
        return json.readTree(actions.andReturn().getResponse().body().asString());
    }

    private HttpTestClient.Actions send(HttpTestClient.Request request, Object payload) throws Exception {
        return mvc.perform(request.header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(payload)));
    }

    private JsonNode get(String path) throws Exception {
        return body(mvc.perform(HttpTestClient.get(path).header("Authorization", auth)).andExpect(status().isOk())).get("data");
    }

    private List<String> nombres(JsonNode lista) {
        List<String> nombres = new ArrayList<>();
        lista.forEach(s -> nombres.add(s.get("name").asText()));
        return nombres;
    }

    private String idDe(JsonNode evento, String nombre) {
        for (JsonNode s : evento.get("subtareas")) {
            if (s.get("name").asText().equals(nombre)) return s.get("id").asText();
        }
        throw new AssertionError("No existe la subtarea " + nombre);
    }

    @Test
    void crearVerEnHoyReprogramarConConflictoYRegistrarEjecucion() throws Exception {
        var cuenta = body(mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("nombre", "Camila", "correo", "flujo" + System.nanoTime() + "@correo.com", "password", "Secreta123"))))).get("data");
        auth = "Bearer " + cuenta.get("accessToken").asText();
        send(put("/api/capacity"), Map.of("limiteHoras", 6)).andExpect(status().isOk());

        // T1: evento con plan inicial de tres gestiones
        var creado = body(send(post("/api/events"), Map.of(
                "nombre", "Boda Camila", "tipo", "Social", "fechaHora", hoy.plusDays(10) + "T18:00:00",
                "subtareas", List.of(
                        Map.of("name", "Salon", "targetDate", hoy.toString(), "estimatedHours", 3),
                        Map.of("name", "Catering", "targetDate", hoy.plusDays(1).toString(), "estimatedHours", 2),
                        Map.of("name", "Musica", "targetDate", hoy.plusDays(3).toString(), "estimatedHours", 1))))
                .andExpect(status().isCreated())).get("data");
        String eventoId = creado.get("id").asText();
        String salon = idDe(creado, "Salon"), catering = idDe(creado, "Catering"), musica = idDe(creado, "Musica");
        assertThat(get("/api/events/" + eventoId + "/progress").get("total").asLong()).isEqualTo(3);
        assertThat(get("/api/events/" + eventoId + "/progress").get("done").asLong()).isZero();

        // T2: Hoy agrupa por plazo y el filtro por evento devuelve lo mismo
        var vista = get("/api/today");
        assertThat(nombres(vista.get("paraHoy"))).containsExactly("Salon");
        assertThat(nombres(vista.get("proximas"))).containsExactly("Catering", "Musica");
        assertThat(get("/api/today?eventId=" + eventoId)).isEqualTo(vista);

        // T3: mover Catering a hoy con 4 h supera el limite (3 + 4 > 6); la previsualizacion y el guardado coinciden
        var aviso = get0(send(post("/api/subtasks/" + catering + "/conflicts/overload"),
                Map.of("targetDate", hoy.toString(), "estimatedHours", 4)).andExpect(status().isOk()));
        assertThat(aviso.get("conflict").asBoolean()).isTrue();
        assertThat(aviso.get("exceedsBy").asDouble()).isEqualTo(1.0);
        var rechazo = body(send(patch("/api/subtasks/" + catering), Map.of("targetDate", hoy.toString(), "estimatedHours", 4))
                .andExpect(status().isConflict()));
        assertThat(rechazo.get("code").asText()).isEqualTo("overload_conflict");
        assertThat(rechazo.get("exceedsBy").asDouble()).isEqualTo(1.0);
        assertThat(nombres(get("/api/today").get("paraHoy"))).as("el rechazo no deja cambios a medias").containsExactly("Salon");

        // Resolucion sugerida: reducir a 3 h cabe justo en el limite
        send(patch("/api/subtasks/" + catering), Map.of("targetDate", hoy.toString(), "estimatedHours", 3)).andExpect(status().isOk());
        assertThat(nombres(get("/api/today").get("paraHoy"))).containsExactlyInAnyOrder("Salon", "Catering");

        // T3: retraso en la busqueda de proveedores: Musica se pospone con su nota y sigue visible
        var pospuesta = get0(send(patch("/api/subtasks/" + musica + "/status"),
                Map.of("status", "POSTPONED", "note", "Retraso en la busqueda de proveedores")).andExpect(status().isOk()));
        assertThat(pospuesta.get("note").asText()).isEqualTo("Retraso en la busqueda de proveedores");
        var musicaEnHoy = get("/api/today").get("proximas").get(0);
        assertThat(musicaEnHoy.get("status").asText()).isEqualTo("POSTPONED");

        // T4: registrar Salon como hecha sale de Hoy, libera su capacidad y sube el progreso
        var hecha = get0(send(patch("/api/subtasks/" + salon + "/status"), Map.of("status", "DONE", "note", "Contrato firmado"))
                .andExpect(status().isOk()));
        assertThat(hecha.get("doneAt").isNull()).isFalse();
        assertThat(nombres(get("/api/today").get("paraHoy"))).containsExactly("Catering");
        var progreso = get("/api/events/" + eventoId + "/progress");
        assertThat(progreso.get("done").asLong()).isEqualTo(1);
        assertThat(progreso.get("total").asLong()).isEqualTo(3);
        assertThat(progreso.get("percentage").asDouble()).isBetween(33.3, 33.34);

        // La capacidad liberada se puede usar, y reabrir la hecha entonces superaria el limite
        send(post("/api/events/" + eventoId + "/subtasks"),
                Map.of("name", "Flores", "targetDate", hoy.toString(), "estimatedHours", 3)).andExpect(status().isCreated());
        assertThat(body(send(patch("/api/subtasks/" + salon + "/status"), Map.of("status", "PENDING"))
                .andExpect(status().isConflict())).get("code").asText()).isEqualTo("overload_conflict");
        assertThat(get("/api/events/" + eventoId + "/progress").get("done").asLong()).as("el rechazo no deshace el registro").isEqualTo(1);

        // El detalle del evento coincide con Hoy y con el progreso
        var detalle = get("/api/events/" + eventoId);
        assertThat(detalle.get("subtareas")).hasSize(4);
        long hechas = 0;
        for (JsonNode s : detalle.get("subtareas")) if ("DONE".equals(s.get("status").asText())) hechas++;
        assertThat(hechas).isEqualTo(get("/api/events/" + eventoId + "/progress").get("done").asLong());
        assertThat(get("/api/events/" + eventoId + "/progress").get("total").asLong()).isEqualTo(4);
    }

    private JsonNode get0(HttpTestClient.Actions actions) throws Exception {
        return body(actions).get("data");
    }
}
