package com.events.infrastructure.adapter.in.rest.controller;
import com.events.support.HttpTestClient;
import jakarta.ws.rs.core.MediaType;
import static com.events.support.HttpTestClient.*;

import com.events.application.port.in.CheckOverloadConflictPort;
import com.events.application.port.in.OverloadCheckResult;
import com.events.infrastructure.utils.constants.MessageConstants;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;

@io.quarkus.test.junit.QuarkusTest
@io.quarkus.test.junit.TestProfile(com.events.support.UnsecuredProfile.class)
class ConflictControllerTest {
    HttpTestClient mvc=new HttpTestClient();
    @io.quarkus.test.InjectMock com.events.infrastructure.security.DatabaseJwtAuthenticationConverter jwtConverter;
    @io.quarkus.test.InjectMock CheckOverloadConflictPort check;
    private final UUID id = UUID.randomUUID();
    private String url() { return "/api/subtasks/" + id + "/conflicts/overload"; }

    @Test
    void devuelveElResultadoDeLaPrevisualizacion() throws Exception {
        when(check.execute(id, LocalDate.parse("2026-11-05"), new BigDecimal("2")))
                .thenReturn(new OverloadCheckResult(true, new BigDecimal("8"), new BigDecimal("6"), new BigDecimal("2")));
        mvc.perform(post(url()).contentType(MediaType.APPLICATION_JSON).content("{\"targetDate\":\"2026-11-05\",\"estimatedHours\":2}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.conflict").value(true))
                .andExpect(jsonPath("$.data.exceedsBy").value(2));
    }

    @Test
    void rechazaBodyVacioConBadRequestEnVezDeError500() throws Exception {
        mvc.perform(post(url()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(MessageConstants.INVALID_REQUEST));
        verifyNoInteractions(check);
    }

    @Test
    void rechazaHorasEnCeroONegativas() throws Exception {
        for (String horas : new String[]{"0", "-10"}) {
            mvc.perform(post(url()).contentType(MediaType.APPLICATION_JSON).content("{\"estimatedHours\":" + horas + "}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(MessageConstants.HORAS_ESTIMADAS_POSITIVE));
        }
        verifyNoInteractions(check);
    }
}
