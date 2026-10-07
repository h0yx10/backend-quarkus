package com.events.domain.entity;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CapacidadDiariaTest {

    private final Organizador organizador = new Organizador(new Usuario("Demo", "demo@x.com", "hash"));

    @Test
    void rechazaLimiteMenorQueUno() {
        assertThatThrownBy(() -> new CapacidadDiaria(organizador, LocalDate.now(), BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rechazaLimiteMayorQueDieciseis() {
        assertThatThrownBy(() -> new CapacidadDiaria(organizador, LocalDate.now(), BigDecimal.valueOf(17)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aceptaLosLimitesEnElBorde() {
        new CapacidadDiaria(organizador, LocalDate.now(), BigDecimal.ONE);
        new CapacidadDiaria(organizador, LocalDate.now(), BigDecimal.valueOf(16));
    }
}
