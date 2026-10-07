package com.events.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;

public record NuevaSubtareaData(String nombre, String descripcion, LocalDate fechaObjetivo, BigDecimal horasEstimadas) {
    public NuevaSubtareaData(String nombre, LocalDate fechaObjetivo, BigDecimal horasEstimadas) {
        this(nombre, null, fechaObjetivo, horasEstimadas);
    }
}
