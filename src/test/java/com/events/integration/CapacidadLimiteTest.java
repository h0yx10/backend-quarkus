package com.events.integration;

import com.events.support.HttpTestClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.MediaType;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static com.events.support.HttpTestClient.*;
import static org.assertj.core.api.Assertions.assertThat;

/** US-12: bajar el limite diario no puede dejar dias ya planificados en sobrecarga. */
@io.quarkus.test.junit.QuarkusTest
class CapacidadLimiteTest {
    HttpTestClient mvc = new HttpTestClient();
    @jakarta.inject.Inject ObjectMapper json;

    private JsonNode body(HttpTestClient.Actions actions) throws Exception {
        return json.readTree(actions.andReturn().getResponse().body().asString());
    }

    private HttpTestClient.Actions putCapacidad(String auth, String horas) {
        return mvc.perform(put("/api/capacity").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"limiteHoras\":" + horas + "}"));
    }

    @Test
    void bajarElLimitePorDebajoDeUnDiaPlanificadoDevuelve409ConLosDias() throws Exception {
        var cuenta = body(mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("nombre", "Camila", "correo", "cap" + System.nanoTime() + "@correo.com", "password", "Secreta123"))))).get("data");
        String auth = "Bearer " + cuenta.get("accessToken").asText();
        String dia = LocalDate.now().plusDays(2).toString();
        mvc.perform(post("/api/events").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Boda\",\"tipo\":\"Social\",\"fechaHora\":\"" + LocalDate.now().plusDays(30) + "T12:00:00\","
                        + "\"subtareas\":[{\"name\":\"Salon\",\"targetDate\":\"" + dia + "\",\"estimatedHours\":4},"
                        + "{\"name\":\"Catering\",\"targetDate\":\"" + dia + "\",\"estimatedHours\":2}]}"))
                .andExpect(status().isCreated());

        var conflicto = body(putCapacidad(auth, "5.5").andExpect(status().isConflict()));
        assertThat(conflicto.get("code").asText()).isEqualTo("capacity_below_planned");
        assertThat(conflicto.get("overloadedDays").get(0).get("date").asText()).isEqualTo(dia);
        assertThat(conflicto.get("overloadedDays").get(0).get("plannedHours").asDouble()).isEqualTo(6.0);
        mvc.perform(get("/api/capacity").header("Authorization", auth))
                .andExpect(jsonPath("$.data.limiteHoras").value(6));

        putCapacidad(auth, "6").andExpect(status().isOk());
        putCapacidad(auth, "8").andExpect(status().isOk());
    }
}
