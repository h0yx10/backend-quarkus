package com.events.infrastructure.adapter.in.rest.controller;
import com.events.support.HttpTestClient;
import com.events.support.EntityIds;
import com.events.support.DatabaseTestClient;
import jakarta.ws.rs.core.MediaType;
import static com.events.support.HttpTestClient.*;


import com.events.application.port.in.*;
import com.events.domain.entity.*;
import com.events.infrastructure.adapter.in.rest.mapper.SubtareaRestMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;

@io.quarkus.test.junit.QuarkusTest
@io.quarkus.test.junit.TestProfile(com.events.support.UnsecuredProfile.class)
class SubtareaDescriptionTest {
    HttpTestClient mvc=new HttpTestClient();
    @io.quarkus.test.InjectMock com.events.infrastructure.security.DatabaseJwtAuthenticationConverter jwtConverter;
    @io.quarkus.test.InjectMock CreateSubtareaPort create;
    @io.quarkus.test.InjectMock ListSubtareasPort list;
    @io.quarkus.test.InjectMock UpdateSubtareaPort update;
    @io.quarkus.test.InjectMock ChangeSubtareaStatusPort changeStatus;
    @io.quarkus.test.InjectMock DeleteSubtareaPort delete;
    private final UUID eventId = UUID.randomUUID();
    @BeforeEach
    void setup() {
        when(create.execute(any(), any(), nullable(String.class), any(), any())).thenAnswer(i -> {
            var evento = new Evento("Boda", "Social", null, null, null, null, null,
                    new Organizador(new Usuario("Camila", "camila@correo.com", "hash")));
            EntityIds.setField(evento, "id", eventId);
            var subtarea = new Subtarea(i.getArgument(1), i.getArgument(2), i.getArgument(3), i.getArgument(4));
            subtarea.asociarEvento(evento);
            return subtarea;
        });
    }
    @Test
    void aceptaPayloadDelFrontendYDevuelveDescription() throws Exception {
        mvc.perform(post("/api/events/" + eventId + "/subtasks").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Confirmar catering","description":"Confirmar menu vegetariano",
                 "targetDate":"2026-10-10","estimatedHours":2}
                """))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.description").value("Confirmar menu vegetariano"))
                .andExpect(jsonPath("$.data.name").value("Confirmar catering"))
                .andExpect(jsonPath("$.data.eventId").value(eventId.toString()))
                .andExpect(jsonPath("$.data.targetDate").value("2026-10-10"))
                .andExpect(jsonPath("$.data.estimatedHours").value(2))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.nombre").doesNotExist())
                .andExpect(jsonPath("$.data.eventoId").doesNotExist())
                .andExpect(jsonPath("$.data.fechaObjetivo").doesNotExist())
                .andExpect(jsonPath("$.data.horasEstimadas").doesNotExist())
                .andExpect(jsonPath("$.data.estado").doesNotExist())
                .andExpect(jsonPath("$.data.nota").doesNotExist());
        verify(create).execute(eventId, "Confirmar catering", "Confirmar menu vegetariano", LocalDate.parse("2026-10-10"), new BigDecimal("2"));
    }
    @Test
    void mantieneCompatibilidadConNombresEnEspanol() throws Exception {
        mvc.perform(post("/api/events/" + eventId + "/subtasks").contentType(MediaType.APPLICATION_JSON).content("""
                {"nombre":"Catering","descripcion":"Confirmar menu","fechaObjetivo":"2026-10-10","horasEstimadas":2}
                """))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.description").value("Confirmar menu"));
    }
    @Test
    void descriptionEsOpcionalYAdmiteNull() throws Exception {
        for (String description : new String[]{"", ",\"description\":null"}) {
            mvc.perform(post("/api/events/" + eventId + "/subtasks").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nombre\":\"Catering\",\"fechaObjetivo\":\"2026-10-10\",\"horasEstimadas\":2" + description + "}"))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.data.description").isEmpty());
        }
    }
    @Test
    void rechazaDescriptionMayorAlLimiteSinInvocarCasoDeUso() throws Exception {
        mvc.perform(post("/api/events/" + eventId + "/subtasks").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Catering\",\"targetDate\":\"2026-10-10\",\"estimatedHours\":2,\"description\":\""+"a".repeat(256)+"\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("La descripcion puede tener maximo 255 caracteres."));
        verifyNoInteractions(create);
    }
    @Test
    void patchAceptaDescripcionEnAmbosIdiomasYLaDevuelve() throws Exception {
        var subtarea = new Subtarea("Catering", "Nueva", LocalDate.parse("2026-10-10"), BigDecimal.ONE);
        var evento = new Evento("Boda", "Social", null, null, null, null, null,
                new Organizador(new Usuario("Camila", "camila@correo.com", "hash")));
        subtarea.asociarEvento(evento);
        when(update.execute(eventId, null, "Nueva", null, null)).thenReturn(subtarea);
        for (String campo : new String[]{"description", "descripcion"}) {
            mvc.perform(patch("/api/subtasks/" + eventId).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"" + campo + "\":\"Nueva\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.description").value("Nueva"));
        }
        verify(update, times(2)).execute(eventId, null, "Nueva", null, null);
    }
    @Test
    void patchValidaLimiteAntesDeEditar() throws Exception {
        mvc.perform(patch("/api/subtasks/" + eventId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\":\"" + "a".repeat(256) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La descripcion puede tener maximo 255 caracteres."));
        verifyNoInteractions(update);
    }
    @Test
    void patchAceptaTodosLosCamposEnIngles() throws Exception {
        var subtarea = new Subtarea("Actualizada", "Nueva", LocalDate.parse("2026-10-12"), new BigDecimal("3"));
        subtarea.asociarEvento(new Evento("Boda", "Social", null, null, null, null, null,
                new Organizador(new Usuario("Camila", "camila@correo.com", "hash"))));
        when(update.execute(eventId, "Actualizada", "Nueva", LocalDate.parse("2026-10-12"), new BigDecimal("3")))
                .thenReturn(subtarea);
        mvc.perform(patch("/api/subtasks/" + eventId).contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Actualizada","description":"Nueva","targetDate":"2026-10-12","estimatedHours":3}
                """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.name").value("Actualizada"))
                .andExpect(jsonPath("$.data.targetDate").value("2026-10-12"))
                .andExpect(jsonPath("$.data.estimatedHours").value(3));
        verify(update).execute(eventId, "Actualizada", "Nueva", LocalDate.parse("2026-10-12"), new BigDecimal("3"));
    }
    @Test
    void cambioDeEstadoUsaStatusYNoteConAliasDeCompatibilidad() throws Exception {
        var subtarea = new Subtarea("Catering", LocalDate.parse("2026-10-12"), BigDecimal.ONE);
        subtarea.asociarEvento(new Evento("Boda", "Social", null, null, null, null, null,
                new Organizador(new Usuario("Camila", "camila@correo.com", "hash"))));
        subtarea.posponer("Esperando");
        when(changeStatus.execute(eventId, EstadoSubtarea.POSTPONED, "Esperando")).thenReturn(subtarea);
        for (String body : new String[]{"{\"status\":\"POSTPONED\",\"note\":\"Esperando\"}",
                "{\"estado\":\"POSTPONED\",\"nota\":\"Esperando\"}"}) {
            mvc.perform(patch("/api/subtasks/" + eventId + "/status").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("POSTPONED"))
                    .andExpect(jsonPath("$.data.note").value("Esperando"))
                    .andExpect(jsonPath("$.data.estado").doesNotExist()).andExpect(jsonPath("$.data.nota").doesNotExist());
        }
    }
}
