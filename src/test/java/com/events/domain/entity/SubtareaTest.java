package com.events.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class SubtareaTest {

    @Test
    void seCreaEnEstadoPending() {
        Subtarea subtarea = new Subtarea("Reservar salon", LocalDate.now(), BigDecimal.ONE);
        assertThat(subtarea.getEstado()).isEqualTo(EstadoSubtarea.PENDING);
    }

    @Test
    void rechazaHorasEstimadasEnCeroOMenos() {
        assertThatThrownBy(() -> new Subtarea("Reservar salon", LocalDate.now(), BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Subtarea("Reservar salon", LocalDate.now(), BigDecimal.valueOf(-1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void marcarHechaRegistraFechaDeCierre() {
        Subtarea subtarea = new Subtarea("Reservar salon", LocalDate.now(), BigDecimal.ONE);
        subtarea.marcarHecha();
        assertThat(subtarea.getEstado()).isEqualTo(EstadoSubtarea.DONE);
        assertThat(subtarea.getDoneAt()).isNotNull();
    }

    @Test
    void posponerGuardaNotaOpcional() {
        Subtarea subtarea = new Subtarea("Reservar salon", LocalDate.now(), BigDecimal.ONE);
        subtarea.posponer("Esperando confirmacion de salon");
        assertThat(subtarea.getEstado()).isEqualTo(EstadoSubtarea.POSTPONED);
        assertThat(subtarea.getNota()).isEqualTo("Esperando confirmacion de salon");

        Subtarea sinNota = new Subtarea("Enviar invitaciones", LocalDate.now(), BigDecimal.ONE);
        sinNota.posponer(null);
        assertThat(sinNota.getNota()).isNull();
    }

    @Test
    void marcarHechaGuardaLaNotaDeEjecucion() {
        Subtarea subtarea = new Subtarea("Reservar salon", LocalDate.now(), BigDecimal.ONE);
        subtarea.marcarHecha("Proveedor confirmo por correo");
        assertThat(subtarea.getNota()).isEqualTo("Proveedor confirmo por correo");
    }

    @Test
    void volverAMarcarHechaConservaLaFechaRealDeCierre() {
        Subtarea subtarea = new Subtarea("Reservar salon", LocalDate.now(), BigDecimal.ONE);
        subtarea.marcarHecha();
        var cierre = subtarea.getDoneAt();

        subtarea.marcarHecha("Agrego detalle");

        assertThat(subtarea.getDoneAt()).isEqualTo(cierre);
        assertThat(subtarea.getNota()).isEqualTo("Agrego detalle");
    }

    @Test
    void omitirLaNotaConservaLaAnteriorYVaciaConCadenaVacia() {
        Subtarea subtarea = new Subtarea("Buscar proveedor", LocalDate.now(), BigDecimal.ONE);
        subtarea.posponer("Esperando proveedor");

        subtarea.marcarHecha(null);
        assertThat(subtarea.getNota()).isEqualTo("Esperando proveedor");

        subtarea.reabrir("");
        assertThat(subtarea.getNota()).isEmpty();
        assertThat(subtarea.getEstado()).isEqualTo(EstadoSubtarea.PENDING);
        assertThat(subtarea.getDoneAt()).isNull();
    }

    @Test
    void rechazaNotaDeMasDeMilCaracteresSinCambiarElEstado() {
        Subtarea subtarea = new Subtarea("Buscar proveedor", LocalDate.now(), BigDecimal.ONE);
        assertThatThrownBy(() -> subtarea.marcarHecha("a".repeat(1001)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La nota puede tener maximo 1000 caracteres.");
        assertThat(subtarea.getEstado()).isEqualTo(EstadoSubtarea.PENDING);
        assertThat(subtarea.getDoneAt()).isNull();
    }
}
