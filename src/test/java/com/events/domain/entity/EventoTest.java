package com.events.domain.entity;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class EventoTest {

    private final Evento evento = new Evento("Boda", "Social", null, null,
            LocalDateTime.of(2026, 12, 1, 18, 0), null, null, null);

    @Test
    void permiteSubtareasHastaElDiaDelEventoYEnFechasPasadas() {
        assertThatCode(() -> evento.validarFechaSubtarea(LocalDate.of(2026, 12, 1))).doesNotThrowAnyException();
        assertThatCode(() -> evento.validarFechaSubtarea(LocalDate.of(2020, 1, 1))).doesNotThrowAnyException();
    }

    @Test
    void rechazaSubtareasDespuesDelDiaDelEvento() {
        assertThatThrownBy(() -> evento.validarFechaSubtarea(LocalDate.of(2026, 12, 2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La fecha objetivo no puede ser posterior a la fecha del evento (2026-12-01).");
    }

    @Test
    void noValidaSiElEventoNoTieneFechaOLaSubtareaNoCambiaDeFecha() {
        Evento sinFecha = new Evento("Boda", "Social", null, null, null, null, null, null);
        assertThatCode(() -> sinFecha.validarFechaSubtarea(LocalDate.of(2030, 1, 1))).doesNotThrowAnyException();
        assertThatCode(() -> evento.validarFechaSubtarea(null)).doesNotThrowAnyException();
    }
}
